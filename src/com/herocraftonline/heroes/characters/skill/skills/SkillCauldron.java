package com.herocraftonline.heroes.characters.skill.skills;
/*
 * TODO: (Updated by Andrew2060 1/17/2012)
 * - Handling for shift-click results: before commenting on whether this is doable or not I would require more knowledge as to why this was blocked in the first place
 * - Automatic configuration generation with defaults: not done yet, basically just make sure all the fields (i.e. items 0-8) exist for each recipe just to be safe although the error handling code should already be able to handle this
 * - Test material-data handling: this is mainly because I was using a weird texture pack where it is not easy to test data values (all the wool colors are the same, don't ask).
 * - Reimplement shapeless recipes (will require further investigation)
 * 
 * Aside from that everything should work, I'm not completely sure about the performance as the code used for recipe generation is fairly tedious (~0.1 sec/call according to profiler)
 * however I tried to limit the amount of times the most tedious portion of the code gets called (alkarin's portion): it should only trigger for
 * recipes/item precraft events where the results are the same as a result inside the config.
 * 
 * At worst, you'd experience an ~3 second lag if everyone on a 100 person server crafted an item using cauldron at once (tested using a bot heh)
 * unfortunately with the clunky way bukkit handles recipes (why one would use a multidimensional array when a single list of itemstacks would be sufficient is beyond me, however, that's just the way it is) there's not much way around the lag, as essentially
 * alkarin's method is one of the ONLY ways to efficiently determine recipe equality when spaces inside recipes are factored into the picture. 
 */

/*
 * Bugs fixed in this version:
 * -Vanilla recipes that share a result with a cauldron recipe not working
 * -Usage of a deprecated ItemStack constructor method
 * -Fixed material data to actually be used, instead of sending an invalid byte to the server that at the very best does nothing
 * -Fixed some inefficient use of variable assignments where configuration instances were triggered inside a for loop repeatedly
 */
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.material.Cauldron;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillCauldron extends PassiveSkill {

	public ArrayList<ShapedRecipe> ShapedCauldronRecipes = new ArrayList<ShapedRecipe>();
	public ArrayList<Integer> CauldronRecipesLevel = new ArrayList<Integer>();
	private File CauldronConfigFile;
	private FileConfiguration CauldronConfig;
	Heroes plugin;

	public SkillCauldron(Heroes plugin) {
		super(plugin, "Cauldron");
		setDescription("You are able to use cauldrons to make cauldron recipes! Visit hc.to/cauldron for more info");
		setArgumentRange(0, 0);
        setTypes(SkillType.ITEM_CREATION, SkillType.ITEM_MODIFYING);
		setEffectTypes(EffectType.BENEFICIAL);
		this.plugin = plugin;
		loadCauldronRecipes();
		Bukkit.getServer().getPluginManager().registerEvents(new SkillListener(this, plugin), plugin);
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.APPLY_TEXT.node(), "");
        node.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        node.set(SkillSetting.LEVEL.node(), 1);

        return node;
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
		FileConfiguration config = getCauldronConfig();
		if (ShapedCauldronRecipes.size() > 0){
			this.ShapedCauldronRecipes.clear();
			this.CauldronRecipesLevel.clear();
		}

		for(int i =0; i<getCauldronConfig().getInt("CauldronRecipes.size"); i++){
			ShapedRecipe shapedRecipe = new ShapedRecipe(new ItemStack(config.getInt("CauldronRecipes."+i+".results.TypeId"),config.getInt("CauldronRecipes."+i+".results.result-amount"),(short)config.getInt("CauldronRecipes."+i+".results.materialData")));
			//Build a recipe from the ground up because Bukkit does not allow for replacement with Material.AIR
			String top = "ABC";			//3 Spaces->3 slots
			String mid = "DEF";
			String bot = "GHI";

			//Determine what is top
			for(int j=0; j<3; j++){
				int id = config.getInt("CauldronRecipes."+i+".ingredients.Materials."+j+".TypeId");
				//If ID is 0, replace with space
				if(id == 0) {
					top.replace(convertInttoChar(j), ' ') ;
				}
			}

			//Determine what is mid
			for(int j=3; j<6; j++){
				int id = config.getInt("CauldronRecipes."+i+".ingredients.Materials."+j+".TypeId");
				if(id == 0) {
					mid.replace(convertInttoChar(j), ' ') ;
				}
			}

			//Determine what is bot
			for(int j=6; j<9; j++){
				int id = config.getInt("CauldronRecipes."+i+".ingredients.Materials."+j+".TypeId");
				if(id == 0) {
					bot.replace(convertInttoChar(j), ' ') ;
				}
			}

			//Set our shaped recipe to have this shape.
			shapedRecipe.shape(top,mid,bot);
			for(int j=0; j<9; j++) {
				//Error handling if configuration requirements not met
				if(!(config.getInt("CauldronRecipes."+i+".ingredients.Materials."+j+".TypeId",0) == 0)) {
					shapedRecipe.setIngredient(convertInttoChar(j), Material.getMaterial(config.getInt("CauldronRecipes."+i+".ingredients.Materials."+j+".TypeId")));
				}
			}
			server.addRecipe(shapedRecipe);
			this.ShapedCauldronRecipes.add(shapedRecipe);

			CauldronRecipesLevel.add(config.getInt("CauldronRecipes."+i+".results.Level"));

		}

	}
	public char convertInttoChar(int i) {
		switch(i) {
		case 0: return 'A';
		case 1: return 'B';
		case 2: return 'C';
		case 3: return 'D';
		case 4: return 'E';
		case 5: return 'F';
		case 6: return 'G';
		case 7: return 'H';
		case 8: return 'I';
		default: return 'J';
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
					//I DO question the usage of a plankblock: obviously it is up to you but....planks burn out REALLY fast (in fact sometimes I didn't even have enough time to right click the cauldron heh)
					//Removed the permission check: it was adding .05-.1 sec delay (believe it or not this is huge ok?)
					//every time someone crafted something and served an redundant purpose considering we already checked for 
					if(cauldron.isFull() && fireblock.getType() == Material.FIRE && plankblock.getType() == Material.LOG){
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
						Recipe recipe = event.getRecipe();
						if(usingCauldronbench.get(i) == false) {
							for (int j=0; j<ShapedCauldronRecipes.size(); j++){
								ShapedRecipe shapedRecipe = ShapedCauldronRecipes.get(j);
								//Moved recipe comparison to a seperate function using alkarin's method
								//Considering how complex it is
								if (!compareRecipes(shapedRecipe, recipe)){
									bCanMake.set(i, true);

								}else{
									bCanMake.set(i, false);
									break;
								}
							}
						}

						if(usingCauldronbench.get(i) == true) {
							for (int j=0; j<ShapedCauldronRecipes.size(); j++){
								ShapedRecipe shapedRecipe = ShapedCauldronRecipes.get(j);

								if (compareRecipes(shapedRecipe, recipe) && CauldronRecipesLevel.get(j) <= sLevel){
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

	/**
	 * Determines if a recipe is equivalent to a given shaped recipe
	 * 
	 * @param ShapedRecipe shapedRecipe
	 * @param Recipe recipe
	 * @return boolean
	 **/
	public boolean compareRecipes(ShapedRecipe shapedRecipe, Recipe recipe) {
		//If the recipe in question isn't even a shaped recipe it won't match
		if(!(recipe instanceof ShapedRecipe)) {
			return false;
		}
		ShapedRecipe comparedRecipe = (ShapedRecipe)recipe;
		//If results don't match, then obviously its not the same 
		if(comparedRecipe.getResult().getTypeId() != shapedRecipe.getResult().getTypeId()) {
			return false;
		}
		/*
		 * The following code shamelessly stolen/VERY slightly (pushing it) adapted (barely at all honestly)
		 * from alkarin (with permission obviously who do you think I am)
		 */
		ShapedRecipe rs[] = {shapedRecipe, comparedRecipe};
		int nncount[] = new int[2]; //Number of non-null itemstacks inside these two recipes (i.e. slots that contain items)
		List<LinkedList<ItemStack>> items = new ArrayList<LinkedList<ItemStack>>();
		for (int k=0;k<rs.length;k++){
			LinkedList<ItemStack> is = new LinkedList<ItemStack>();
			items.add(is);
			String[] shape = rs[k].getShape();
			Map<Character, ItemStack> ingMap = rs[k].getIngredientMap();
			boolean firstItemFound = false;
			/// Add all items to the list, Starting from the first non null/air item
			for (int i = 0;i < shape.length;i++){
				for (int j=0;j < shape[i].length();j++){
					if (shape[i] == null){
						continue;
					}
					ItemStack item = ingMap.get(shape[i].charAt(j));
					if (item != null){
						nncount[k]++;
						firstItemFound = true;
						if (item.getType() == Material.AIR)
							item = null;

					}
					if (firstItemFound){
						is.add(item);
					}
				}
			}
		}
		/// different recipes b/c they have different number of non null itemstacks
		if (nncount[0]!= nncount[1]){
			return false;
		}

		/// Remove nulls / air from the end of the lists
		for (int i=0;i<items.size();i++){
			LinkedList<ItemStack> is = items.get(i);
			for (int j=is.size()-1;j>=0;j--){
				if (is.get(j) == null)
					is.remove(j);
			}
		}
		/// Now that we have 2 similar lists, check them
		/// we can start with the easy.. are they the same size
		if (items.get(0).size() != items.get(1).size()){
			return false;
		}
		ItemStack i1,i2;
		for (int i=0;i< items.get(0).size();i++){
			i1 = items.get(0).get(i); i2 = items.get(1).get(i);
			if (i1 == null && i2 == null){ /// thats the same
				continue;
			} else if (i1 == null || i2== null){
				return false;
			} else if (!i1.equals(i2)){
				return false;
			}
		}
		return true;
	}
}