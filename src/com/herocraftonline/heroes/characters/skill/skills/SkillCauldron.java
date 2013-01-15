package com.herocraftonline.heroes.characters.skill.skills;
//TODO: Shape recipes. Result collection for shift clicks. Fix config format. Stream line code, and arrange to a Heroes format. 
import java.io.File;
import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.material.Cauldron;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Setting;

public class SkillCauldron extends PassiveSkill {

	public ArrayList<ShapedRecipe> ShapedCauldronRecipes = new ArrayList<ShapedRecipe>();
	public ArrayList<Integer> CauldronRecipesLevel = new ArrayList<Integer>();
	private File CauldronConfigFile;
	private FileConfiguration CauldronConfig;
	Heroes plugin;

	public SkillCauldron(Heroes plugin) {
		super(plugin, "Cauldron");
		setDescription("You are able to use cauldrons to make cauldron recipes!");
		setArgumentRange(0, 0);
		setTypes(SkillType.KNOWLEDGE, SkillType.ITEM);
		setEffectTypes(EffectType.BENEFICIAL);
		this.plugin = plugin;
		loadCauldronRecipes();
		Bukkit.getServer().getPluginManager().registerEvents(new SkillListener(this, plugin), plugin);
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection section = super.getDefaultConfig();
		section.set(Setting.LEVEL.node(), 1);
		return section;
	}

	public void loadCauldronConfig() {
		if (CauldronConfigFile == null) {
			CauldronConfigFile = new File(plugin.getDataFolder(), "CauldronConfig.yml");
		}

		CauldronConfig = YamlConfiguration.loadConfiguration(CauldronConfigFile);
	}

	public FileConfiguration getCauldronConfig() {
		if (CauldronConfig == null) {
			this.loadCauldronConfig();
		}
		return CauldronConfig;
	}

	public void loadCauldronRecipes() {
		Server server = plugin.getServer();
		if (ShapedCauldronRecipes.size() > 0){
			this.ShapedCauldronRecipes.clear();
			this.CauldronRecipesLevel.clear();
		}

		for(int i =0; i<getCauldronConfig().getInt("CauldronRecipes.size"); i++){
			ShapedRecipe shapedRecipe = new ShapedRecipe(new ItemStack(getCauldronConfig().getInt("CauldronRecipes."+i+".results.TypeId"),getCauldronConfig().getInt("CauldronRecipes."+i+".results.result-amount"),(short)getCauldronConfig().getInt("CauldronRecipes."+i+".results.materialData")));
			//Build a recipe from the ground up because Bukkit does not allow for replacement with Material.AIR
			String top = "012";			//3 Spaces->3 slots
			String mid = "345";
			String bot = "678";
			FileConfiguration config = getCauldronConfig();
			
			//Determine what is top
			for(int j=0; j<3; j++){
				int id = config.getInt("CauldronRecipes."+i+".ingredients.Materials."+j+".TypeId");
				//If ID is 0, replace with space
				if(id == 0) {
					top.replace(j + "", " ") ;
				}
			}
			
			//Determine what is mid
			for(int j=3; j<6; j++){
				int id = config.getInt("CauldronRecipes."+i+".ingredients.Materials."+j+".TypeId");
				if(id == 0) {
					mid.replace(j + "", " "	) ;
				}
			}
			
			//Determine what is bot
			for(int j=6; j<9; j++){
				int id = config.getInt("CauldronRecipes."+i+".ingredients.Materials."+j+".TypeId");
				if(id == 0) {
					bot.replace(j + "", " ") ;
				}
			}
			
			//Set our shaped recipe to have this space.
			shapedRecipe.shape(top,mid,bot);
			for(int j=0; j<9; j++) {
				if(config.getInt("CauldronRecipes."+i+".ingredients.Materials."+j+".TypeId") == 0) {
					continue;
				} else {
					shapedRecipe.setIngredient((char) j, Material.getMaterial(getCauldronConfig().getInt("CauldronRecipes."+i+".ingredients.Materials."+j+".TypeId")));
				}
			}
			this.ShapedCauldronRecipes.add(shapedRecipe);
			
			CauldronRecipesLevel.add(config.getInt("CauldronRecipes."+i+".results.Level"));
			server.addRecipe(ShapedCauldronRecipes.get(ShapedCauldronRecipes.size() -1));

		}

	}

	public static void openCauldron(Player player) {
		player.openWorkbench(null, true);
	}

	public class SkillListener implements Listener {

		private final Skill skill;
		private final ArrayList<Player> player = new ArrayList<Player>();
		private final ArrayList<Boolean> usingCauldronbench = new ArrayList<Boolean>();
		private final ArrayList<Boolean> bCanMake = new ArrayList<Boolean>();

		public SkillListener(Skill skill, Heroes plugin) {
			this.skill = skill;
		}

		//Is player clicking cauldron or workbench? Can they use a cauldron? Adds player(s) to iterators, and assign booleans.
		@EventHandler(priority = EventPriority.LOW)
		public void onPlayerInteract(PlayerInteractEvent event) {
			if (event.getClickedBlock() == null || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
				return;
			}
			Hero hero = plugin.getCharacterManager().getHero(event.getPlayer());

			if(event.getClickedBlock().getType() == Material.WORKBENCH){
				if(!player.contains(event.getPlayer())){
					player.add(event.getPlayer());
					usingCauldronbench.add(player.size()-1, false);
					bCanMake.add(player.size()-1, false);
				}
			}

			if (!hero.canUseSkill(skill) && event.getClickedBlock().getType() == Material.CAULDRON) {
				event.setCancelled(true);
				event.setUseInteractedBlock(Result.DENY);
			}

			if(hero.canUseSkill(skill) && event.getClickedBlock().getType() == Material.CAULDRON){
				if(!player.contains(event.getPlayer())){

					Location loc = new Location(event.getPlayer().getWorld(),event.getClickedBlock().getLocation().getBlockX(),event.getClickedBlock().getLocation().getBlockY() - 1,event.getClickedBlock().getLocation().getBlockZ());
					Block fireblock = event.getClickedBlock().getLocation().getBlock().getRelative(BlockFace.DOWN);
					Block plankblock = loc.getBlock().getRelative(BlockFace.DOWN);
					Cauldron cauldron = (org.bukkit.material.Cauldron) event.getClickedBlock().getState().getData();

					if(cauldron.isFull() && fireblock.getType() == Material.FIRE && plankblock.getType() == Material.WOOD && event.getPlayer().hasPermission("cauldronbench.user.alchemist")){
						player.add(event.getPlayer());
						usingCauldronbench.add(player.size()-1, true);
						bCanMake.add(player.size()-1, false);
						openCauldron(event.getPlayer());
					}
				}	
			}
		}

		//Grabs items in crafting view. Is the player using a workbench or cauldronbench? Is the recipe suitable to be made in work area? Is Player high enough level to make recipe?
		@EventHandler
		public void openCauldronevent(PrepareItemCraftEvent event){
			if(event.getInventory().getType() != InventoryType.WORKBENCH) {
				return;
			}

			for(int i = 0; i < player.size(); i++){
				for(int v = 0; v < event.getViewers().size(); v++){
					if (event.getViewers().get(v) == player.get(i)){

						Hero hero = plugin.getCharacterManager().getHero(player.get(i));
						int sLevel = hero.getLevel(hero.getSecondClass());

						if(usingCauldronbench.get(i) == false) {
							for (int j=0; j<ShapedCauldronRecipes.size(); j++){
								if (event.getRecipe() != ShapedCauldronRecipes.get(j)){
									bCanMake.set(i, true);

								}else{
									bCanMake.set(i, false);
									break;
								}
							}
						}

						if(usingCauldronbench.get(i) == true) {
							for (int j=0; j<ShapedCauldronRecipes.size(); j++){
								if (event.getRecipe() == ShapedCauldronRecipes.get(j) && CauldronRecipesLevel.get(j) >= sLevel){
									bCanMake.set(i, true);
									break;										
								}else{
									bCanMake.set(i, false);
								}
							}
						}
					}	
					if (!bCanMake.get(i) && event.getViewers().get(v) == player.get(i)){
						event.getInventory().setResult(new ItemStack(Material.AIR));
						break;
					}
				}
			}
		}

		//Deny ShiftClick at the moment till proper coding of result collection can occur. Alchemist must click per result. (Prevents bugs till fixed)
		//Possible fix in thread: http://forums.bukkit.org/threads/cant-get-amount-of-shift-click-craft-item.79090/
		@EventHandler
		public void onCraftItemEvent(CraftItemEvent event) {
			if (!player.contains(event.getWhoClicked())){
				return;
			}

			for(int i=0; i<player.size(); i++){	
				if (player.get(i) == event.getWhoClicked()){
					if (usingCauldronbench.get(i) == true) {

						ItemStack item = event.getCurrentItem();
						for (int j=0; j<ShapedCauldronRecipes.size(); j++){
							if (item.getTypeId() == ShapedCauldronRecipes.get(j).getResult().getTypeId() && event.isShiftClick()){
								player.get(i).sendMessage(ChatColor.RED+"You can't ShiftClick cauldron recipes at this time!");
								event.setCancelled(true);
								break;
							}
						}
					}
				}
			}
		}

		//Flush iterators onInventoryCloseEvent if iterator contains player.
		@EventHandler
		public void onInventoryCloseEvent(InventoryCloseEvent event){
			if(!player.contains(event.getPlayer())){
				return;
			}

			for(int i = 0; i < player.size(); i++){
				if (player.get(i) == event.getPlayer()){
					usingCauldronbench.set(i, false);
					bCanMake.set(i, false);
					player.remove(i);
					usingCauldronbench.remove(i);
					bCanMake.remove(i);
				}
			}
		}

	}

	@Override
	public String getDescription(Hero hero) {
		return getDescription();
	}
}