package org.usfirst.frc.team449.robot.mechanism.topcommands.shooter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import org.jetbrains.annotations.Nullable;
import org.usfirst.frc.team449.robot.interfaces.subsystem.Intake.SubsystemIntake;
import org.usfirst.frc.team449.robot.interfaces.subsystem.Intake.commands.SetIntakeMode;
import org.usfirst.frc.team449.robot.interfaces.subsystem.Shooter.SubsystemShooter;
import org.usfirst.frc.team449.robot.interfaces.subsystem.Shooter.commands.TurnAllOff;
import org.usfirst.frc.team449.robot.interfaces.subsystem.solenoid.SubsystemSolenoid;
import org.usfirst.frc.team449.robot.interfaces.subsystem.solenoid.commands.SolenoidReverse;
import org.usfirst.frc.team449.robot.util.YamlCommandGroupWrapper;

/**
 * Command group for intaking balls from the ground. Stops flywheel, runs static intake, runs dynamic intake, lowers
 * intake, and stops feeder.
 */
@JsonIdentityInfo(generator = ObjectIdGenerators.StringIdGenerator.class)
public class LoadShooter <T extends SubsystemIntake & SubsystemSolenoid> extends YamlCommandGroupWrapper {

	/**
	 * Constructs a LoadShooter command group
	 *
	 * @param subsystemShooter shooter subsystem. Can be null.
	 * @param intakeSubsystem  intake subsystem. Can be null.
	 */
	@JsonCreator
	public LoadShooter(@Nullable SubsystemShooter subsystemShooter,
	                   @Nullable T intakeSubsystem) {
		if (subsystemShooter != null) {
			addParallel(new TurnAllOff(subsystemShooter));
		}
		if (intakeSubsystem != null) {
			addParallel(new SolenoidReverse(intakeSubsystem));
			addParallel(new SetIntakeMode(intakeSubsystem, SubsystemIntake.IntakeMode.IN_FAST));
		}
	}
}
