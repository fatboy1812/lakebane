// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.db.handlers;

import engine.Enum;
import engine.gameManager.DbManager;
import engine.gameManager.PowersManager;
import engine.powers.EffectsBase;
import engine.powers.effectmodifiers.*;
import org.pmw.tinylog.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;

public class dbEffectsBaseHandler extends dbHandlerBase {

    public dbEffectsBaseHandler() {

	}

	public static ArrayList<EffectsBase> getAllEffectsBase() {

		ArrayList<EffectsBase> effectList = new ArrayList<>();

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement prepareStatement = connection.prepareStatement("SELECT * FROM static_power_effectbase ORDER BY `IDString` DESC")) {

			ResultSet rs = prepareStatement.executeQuery();

			while (rs.next()) {
				EffectsBase effectBase = new EffectsBase(rs);
				effectList.add(effectBase);
			}
		} catch (SQLException e) {
			Logger.error(e.toString());
		}

		return effectList;
	}

	public static void cacheAllEffectModifiers() {

		String IDString;
		AbstractEffectModifier abstractEffectModifier = null;

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement prepareStatement = connection.prepareStatement("SELECT * FROM static_power_effectmod")) {

			ResultSet rs = prepareStatement.executeQuery();

			while (rs.next()) {

				IDString = rs.getString("IDString");
				EffectsBase effectBase = PowersManager.getEffectByIDString(IDString);
				Enum.ModType modifier = Enum.ModType.GetModType(rs.getString("modType"));

				//combine item prefix and suffix effect modifiers

				abstractEffectModifier = getCombinedModifiers(abstractEffectModifier, rs, effectBase, modifier);

				if (abstractEffectModifier != null) {

					if (EffectsBase.modifiersMap.containsKey(effectBase.getIDString()) == false)
						EffectsBase.modifiersMap.put(effectBase.getIDString(), new HashSet<>());

					EffectsBase.modifiersMap.get(effectBase.getIDString()).add(abstractEffectModifier);

				}

			}

		} catch (Exception e) {
			Logger.error(e);
		}

	}

	private static AbstractEffectModifier getCombinedModifiers(AbstractEffectModifier abstractEffectModifier, ResultSet rs, EffectsBase effectBase, Enum.ModType modifier) throws SQLException {
		switch (modifier) {
			case AdjustAboveDmgCap:
				abstractEffectModifier = new AdjustAboveDmgCapEffectModifier(rs);
				break;
			case Ambidexterity:
				abstractEffectModifier = new AmbidexterityEffectModifier(rs);
				break;
			case AnimOverride:
				break;
			case ArmorPiercing:
				abstractEffectModifier = new ArmorPiercingEffectModifier(rs);
				break;
			case AttackDelay:
				abstractEffectModifier = new AttackDelayEffectModifier(rs);
				break;
			case Attr:
				abstractEffectModifier = new AttributeEffectModifier(rs);
				break;
			case BlackMantle:
				abstractEffectModifier = new BlackMantleEffectModifier(rs);
				break;
			case BladeTrails:
				abstractEffectModifier = new BladeTrailsEffectModifier(rs);
				break;
			case Block:
				abstractEffectModifier = new BlockEffectModifier(rs);
				break;
			case BlockedPowerType:
				abstractEffectModifier = new BlockedPowerTypeEffectModifier(rs);
				break;
			case CannotAttack:
				abstractEffectModifier = new CannotAttackEffectModifier(rs);
				break;
			case CannotCast:
				abstractEffectModifier = new CannotCastEffectModifier(rs);
				break;
			case CannotMove:
				abstractEffectModifier = new CannotMoveEffectModifier(rs);
				break;
			case CannotTrack:
				abstractEffectModifier = new CannotTrackEffectModifier(rs);
				break;
			case Charmed:
				abstractEffectModifier = new CharmedEffectModifier(rs);
				break;
			case ConstrainedAmbidexterity:
				abstractEffectModifier = new ConstrainedAmbidexterityEffectModifier(rs);
				break;
			case DamageCap:
				abstractEffectModifier = new DamageCapEffectModifier(rs);
				break;
			case DamageShield:
				abstractEffectModifier = new DamageShieldEffectModifier(rs);
				break;
			case DCV:
				abstractEffectModifier = new DCVEffectModifier(rs);
				break;
			case Dodge:
				abstractEffectModifier = new DodgeEffectModifier(rs);
				break;
			case DR:
				abstractEffectModifier = new DREffectModifier(rs);
				break;
			case Durability:
				abstractEffectModifier = new DurabilityEffectModifier(rs);
				break;
			case ExclusiveDamageCap:
				abstractEffectModifier = new ExclusiveDamageCapEffectModifier(rs);
				break;
			case Fade:
				abstractEffectModifier = new FadeEffectModifier(rs);
				break;
			case Fly:
				abstractEffectModifier = new FlyEffectModifier(rs);
				break;
			case Health:
				abstractEffectModifier = new HealthEffectModifier(rs);
				break;
			case HealthFull:
				abstractEffectModifier = new HealthFullEffectModifier(rs);
				break;
			case HealthRecoverRate:
				abstractEffectModifier = new HealthRecoverRateEffectModifier(rs);
				break;
			case IgnoreDamageCap:
				abstractEffectModifier = new IgnoreDamageCapEffectModifier(rs);
				break;
			case IgnorePassiveDefense:
				abstractEffectModifier = new IgnorePassiveDefenseEffectModifier(rs);
				break;
			case ImmuneTo:
				abstractEffectModifier = new ImmuneToEffectModifier(rs);
				break;
			case ImmuneToAttack:
				abstractEffectModifier = new ImmuneToAttackEffectModifier(rs);
				break;
			case ImmuneToPowers:
				abstractEffectModifier = new ImmuneToPowersEffectModifier(rs);
				break;
			case Invisible:
				abstractEffectModifier = new InvisibleEffectModifier(rs);
				break;
			case ItemName:
				abstractEffectModifier = new ItemNameEffectModifier(rs);
				if (((ItemNameEffectModifier) abstractEffectModifier).name.isEmpty())
					break;
				if (effectBase != null)
					effectBase.setName((((ItemNameEffectModifier) abstractEffectModifier).name));
				break;
			case Mana:
				abstractEffectModifier = new ManaEffectModifier(rs);
				break;
			case ManaFull:
				abstractEffectModifier = new ManaFullEffectModifier(rs);
				break;
			case ManaRecoverRate:
				abstractEffectModifier = new ManaRecoverRateEffectModifier(rs);
				break;
			case MaxDamage:
				abstractEffectModifier = new MaxDamageEffectModifier(rs);
				break;
			case MeleeDamageModifier:
				abstractEffectModifier = new MeleeDamageEffectModifier(rs);
				break;
			case MinDamage:
				abstractEffectModifier = new MinDamageEffectModifier(rs);
				break;
			case NoMod:
				abstractEffectModifier = new NoModEffectModifier(rs);
				break;
			case OCV:
				abstractEffectModifier = new OCVEffectModifier(rs);
				break;
			case Parry:
				abstractEffectModifier = new ParryEffectModifier(rs);
				break;
			case PassiveDefense:
				abstractEffectModifier = new PassiveDefenseEffectModifier(rs);
				break;
			case PowerCost:
				abstractEffectModifier = new PowerCostEffectModifier(rs);
				break;
			case PowerCostHealth:
				abstractEffectModifier = new PowerCostHealthEffectModifier(rs);
				break;
			case PowerDamageModifier:
				abstractEffectModifier = new PowerDamageEffectModifier(rs);
				break;
			case ProtectionFrom:
				abstractEffectModifier = new ProtectionFromEffectModifier(rs);
				break;
			case Resistance:
				abstractEffectModifier = new ResistanceEffectModifier(rs);
				break;
			case ScaleHeight:
				abstractEffectModifier = new ScaleHeightEffectModifier(rs);
				break;
			case ScaleWidth:
				abstractEffectModifier = new ScaleWidthEffectModifier(rs);
				break;
			case ScanRange:
				abstractEffectModifier = new ScanRangeEffectModifier(rs);
				break;
			case SeeInvisible:
				abstractEffectModifier = new SeeInvisibleEffectModifier(rs);
				break;
			case Silenced:
				abstractEffectModifier = new SilencedEffectModifier(rs);
				break;
			case Skill:
				abstractEffectModifier = new SkillEffectModifier(rs);
				break;
			case Slay:
				abstractEffectModifier = new SlayEffectModifier(rs);
				break;
			case Speed:
				abstractEffectModifier = new SpeedEffectModifier(rs);
				break;
			case SpireBlock:
				abstractEffectModifier = new SpireBlockEffectModifier(rs);
				break;
			case Stamina:
				abstractEffectModifier = new StaminaEffectModifier(rs);
				break;
			case StaminaFull:
				abstractEffectModifier = new StaminaFullEffectModifier(rs);
				break;
			case StaminaRecoverRate:
				abstractEffectModifier = new StaminaRecoverRateEffectModifier(rs);
				break;
			case Stunned:
				abstractEffectModifier = new StunnedEffectModifier(rs);
				break;
			case Value:
				abstractEffectModifier = new ValueEffectModifier(rs);
				if (effectBase != null) {
					ValueEffectModifier valueEffect = (ValueEffectModifier) abstractEffectModifier;
					effectBase.setValue(valueEffect.minMod);
				}
				break;
			case WeaponProc:
				abstractEffectModifier = new WeaponProcEffectModifier(rs);
				break;
			case WeaponRange:
				abstractEffectModifier = new WeaponRangeEffectModifier(rs);
				break;
			case WeaponSpeed:
				abstractEffectModifier = new WeaponSpeedEffectModifier(rs);
				break;

		}
		return abstractEffectModifier;
	}
}
