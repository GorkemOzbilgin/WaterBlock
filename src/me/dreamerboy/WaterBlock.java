package me.dreamerboy;

import java.util.*;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ComboAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.WaterAbility;
import com.projectkorra.projectkorra.ability.util.Collision;
import com.projectkorra.projectkorra.ability.util.ComboManager.AbilityInformation;
import com.projectkorra.projectkorra.ability.util.ComboUtil;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.TempBlock;
import com.projectkorra.projectkorra.waterbending.SurgeWave;
import com.projectkorra.projectkorra.waterbending.Torrent;

public class WaterBlock extends WaterAbility implements AddonAbility, ComboAbility {
	
	private static final String PATH = "ExtraAbilities.DreamerBoy.Water.WaterBlock.";
	private final List<BlockingWave> waves = new ArrayList<>();
	private final Map<BlockAbility, Integer> blockAbilities = new HashMap<>();
	
	@Attribute(Attribute.COOLDOWN)
	private long cooldown;
	@Attribute(Attribute.DURATION)
	private long duration;
	@Attribute(Attribute.RADIUS)
	private double radius;
	@Attribute(Attribute.KNOCKBACK)
	private double knockback;
	private boolean cooldownOnOutOfTheSightView, blocked;

	public WaterBlock(final Player player) {
		super(player);
		
		if(!this.bPlayer.canBendIgnoreBinds(this) || hasAbility(player, WaterBlock.class)) {
			return;
		}
		
		setFields();
		start();
	}
	
	private void setFields() {
		this.cooldown = ConfigManager.getConfig().getLong(PATH + "Cooldown.Cooldown");
		this.cooldownOnOutOfTheSightView = ConfigManager.getConfig().getBoolean(PATH + "Cooldown.CooldownOnOutOfTheSightView");
		this.duration = ConfigManager.getConfig().getLong(PATH + "Duration");
		this.knockback = ConfigManager.getConfig().getDouble(PATH + "Knockback");
		this.radius = ConfigManager.getConfig().getDouble(PATH + "Radius");
		
		ConfigManager.getConfig().getStringList(PATH + "BlockAbilities").stream()
																		.map(BlockAbility::convert)
																		.filter(Objects::nonNull)
																		.forEach(abil -> this.blockAbilities.put(abil, 0));
	}

	@Override
	public long getCooldown() {
		return this.cooldown;
	}

	@Override
	public Location getLocation() {
		return this.player.getLocation();
	} 

	@Override
	public String getName() {
		return "WaterBlock";
	}

	@Override
	public boolean isHarmlessAbility() {
		return false;
	}

	@Override
	public boolean isSneakAbility() {
		return true;
	}
	
	@Override
	public void remove() {
		super.remove();
		if(this.blocked)
			this.bPlayer.addCooldown(this);
	}

	@Override
	public void progress() {
		if(!this.bPlayer.canBendIgnoreBinds(this)) {
			remove();
			return;
		} else if(this.waves.isEmpty()) {
			if(!this.player.isSneaking() || (this.duration > 0 && System.currentTimeMillis() > this.getStartTime() + this.duration)) {
				remove();
				return;
			}
		}
		
		collision();
		this.waves.removeIf(wave -> !wave.run());
	}
	
	@Override
	public double getCollisionRadius() {
		return this.radius;
	}
	
	private void collision() {
		final CoreAbility waterBlock = CoreAbility.getAbility(WaterBlock.class);
		
		this.blockAbilities.keySet().stream()
		.map(block -> block.ability)
		.forEach(abil -> ProjectKorra.getCollisionManager().addCollision(new Collision(waterBlock, abil, false, true)));
	}
	
	private BlockAbility get(final CoreAbility ability) {
		final Optional<BlockAbility> optional = this.blockAbilities.keySet().stream().filter(block -> block.ability.getName().equalsIgnoreCase(ability.getName())).findFirst();
        return optional.orElse(null);
    }
	
	@Override
	public void handleCollision(final Collision collision) {
		if(collision.getAbilitySecond() instanceof Torrent) {
			if(!((Torrent) collision.getAbilitySecond()).isLaunching()) {
				return;
			}
		}
		
		final Vector dir = this.player.getLocation().getDirection();
		final Vector otherVec = collision.getAbilitySecond().getLocation().toVector().subtract(this.player.getLocation().toVector());
		final double angle = Math.toDegrees(Math.acos(dir.dot(otherVec) / (dir.length() * otherVec.length())));
		
		if(angle > 60) {
			this.player.sendMessage(Element.WATER.getColor() + "Because of an attack that is out of the sight view, ability got cancelled.");
			if(this.cooldownOnOutOfTheSightView && !this.blocked)
				this.blocked = true;
			remove();
		} else {
			final BlockAbility block = get(collision.getAbilitySecond());
			if(block == null)
				return;
			
			Location location = collision.getAbilitySecond().getLocation();
			if(collision.getAbilitySecond().getLocations().size() > 1) {
				location = collision.getAbilitySecond().getLocations().stream()
		                .min(Comparator.comparingDouble(loc -> loc.distance(collision.getAbilityFirst().getLocation())))
		                .orElse(collision.getAbilitySecond().getLocation());
			}
			
			if(location == null)
				return;
			
			boolean removeFlag = false;
			this.blockAbilities.put(block, this.blockAbilities.get(block)+1);
			if(this.blockAbilities.get(block) >= block.amount)
				removeFlag = true;
			this.waves.add(new BlockingWave(location, block.range, removeFlag));
			this.blocked = true;
		}
	}
	
	@Override
	public Object createNewComboInstance(final Player player) {
		return new WaterBlock(player);
	}

	@Override
	public ArrayList<AbilityInformation> getCombination() {
		return ComboUtil.generateCombinationFromList(this, ConfigManager.defaultConfig.get().getStringList("ExtraAbilities.DreamerBoy.Water.WaterBlock.Combination"));
	}

	@Override
	public String getAuthor() {
		return "DreamerBoy/Dramaura & Hiro3";
	}

	@Override
	public String getVersion() {
		return "2.2";
	}
	
	@Override
	public void load() {
		FileConfiguration config = ConfigManager.defaultConfig.get();
		config.addDefault("ExtraAbilities.DreamerBoy.Water.WaterBlock.Cooldown.Cooldown", 8000);
		config.addDefault("ExtraAbilities.DreamerBoy.Water.WaterBlock.Cooldown.CooldownOnOutOfTheSightView", true);
		config.addDefault("ExtraAbilities.DreamerBoy.Water.WaterBlock.Duration", 0);
		config.addDefault("ExtraAbilities.DreamerBoy.Water.WaterBlock.Knockback", 2);
		config.addDefault("ExtraAbilities.DreamerBoy.Water.WaterBlock.Radius", 2);
		config.addDefault("ExtraAbilities.DreamerBoy.Water.WaterBlock.BlockAbilities", Arrays.asList("Torrent:8:1", "Surge:8:1", "WaterManipulation:4:3"));
		config.addDefault("ExtraAbilities.DreamerBoy.Water.WaterBlock.Combination", Arrays.asList("PhaseChange:SNEAK_DOWN", "PhaseChange:LEFT_CLICK"));
		ConfigManager.defaultConfig.save();
		
		config = ConfigManager.languageConfig.get();
		config.addDefault("Abilities.Water.Combo.WaterBlock.Description", "With this ability, you can re-manipulate the water within incoming water abilities to create a massive wave that pushes all enemies in front of it.");
		config.addDefault("Abilities.Water.Combo.WaterBlock.Instructions", "PhaseChange (Hold Sneak) > PhaseChange (Left Click)");
		ConfigManager.languageConfig.save();
		
		ProjectKorra.log.info(getName() + " " + getVersion() + " by " + getAuthor() + " enabled! ");
	}

	@Override
	public void stop() {
		super.remove();
		ProjectKorra.log.info(getName() + " " + getVersion() + " by " + getAuthor() + " disabled! ");
	}
	
	private static class BlockAbility {
		private final CoreAbility ability;
		private final int amount;
		private final double range;
		
		public BlockAbility(final CoreAbility ability, final double range, final int amount) {
			this.ability = ability;
			this.range = range;
			this.amount = amount;
		}
		
		public static BlockAbility convert(final String configLine) {
			final String[] str = configLine.split(":");
			if(str.length != 3 || CoreAbility.getAbility(str[0].trim()) == null || !isNumeric(str[1].trim()) || !isNumeric(str[2].trim()))
				return null;
			final double range = Double.parseDouble(str[1].trim());
			final int amount = Integer.parseInt(str[2].trim());
			if(range <= 0 || amount <= 0) return null;
			return new BlockAbility(getAbility(str[0]), range, amount);
		}
		
		/*
		 * We cannot get the correct Torrent or Surge class by using CoreAbility#getAbility
		 */
		private static CoreAbility getAbility(final String str) {
			if(str.equalsIgnoreCase("Torrent"))
				return CoreAbility.getAbility(Torrent.class);
			else if(str.equalsIgnoreCase("Surge"))
				return CoreAbility.getAbility(SurgeWave.class);
			else 
				return CoreAbility.getAbility(str);
		}
		
		private static boolean isNumeric(final String strNum) {
		    if (strNum == null) {
		        return false;
		    }
		    try {
		    	Double.parseDouble(strNum);
		    } catch (final NumberFormatException nfe) {
		        return false;
		    }
		    return true;
		}
	}

	private class BlockingWave {
		
		private final Location location;
		private final Vector dir;
		private double range;
        private final double maxRange;
		private final boolean removeFlag;
		
		public BlockingWave(final Location origin, final double maxRange, final boolean removeFlag) {
			this.location = origin.clone();
			this.maxRange = maxRange;
			this.removeFlag = removeFlag;
			
			this.dir = origin.clone().toVector().subtract(player.getLocation().toVector());
		}
		
		private boolean run() {
			this.range++;
			if(this.range > this.maxRange) {
				if(this.removeFlag)
					WaterBlock.this.remove();
				return false;
			}
			
			this.playEffect(this.location, this.range);
			
			for(Entity entity : GeneralMethods.getEntitiesAroundPoint(this.location, 3D)) {
				if ((entity instanceof LivingEntity) && (entity.getEntityId() != WaterBlock.this.player.getEntityId())) {
					GeneralMethods.setVelocity(WaterBlock.this, entity, entity.getLocation().toVector().subtract(this.location.toVector()).normalize().multiply(WaterBlock.this.knockback));
				}
			}
			
			this.location.add(this.dir.normalize().multiply(1));
			
			return true;
		}
		
		private void playEffect(final Location loc, final double r) {
			for (double theta = 0; theta < 360; theta += (360 / (18+r))) {
				Vector vector;
				
				if (loc.getDirection().getX() == -0.0 && loc.getDirection().getY() == -0.0 && loc.getDirection().getZ() == 1.0) {
					final Vector tmpVector = WaterBlock.this.player.getLocation().toVector().subtract(loc.clone().toVector()).normalize();
					vector = GeneralMethods.getOrthogonalVector(tmpVector, theta, r);
				} else {
					vector = GeneralMethods.getOrthogonalVector(loc.getDirection(), theta, r);
				}
				
				final Location location = loc.clone().add(vector);
				if (isAir(location.getBlock().getType()) || isWater(location.getBlock()) && TempBlock.isTempBlock(location.getBlock())) {
					new TempBlock(location.getBlock(), Material.WATER).setRevertTime(1000);
					location.getWorld().spawnParticle(Particle.SPLASH, location,1, 1.15F, 1.15F, 1.15F, 0.06F);
				}
			}
		}
	}
}
