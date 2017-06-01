package org.usfirst.frc.team449.robot.interfaces.drive.unidirectional.commands;

import edu.wpi.first.wpilibj.command.CommandGroup;
import org.usfirst.frc.team449.robot.interfaces.drive.unidirectional.UnidirectionalDrive;

/**
 * Drive forward at constant speed then stop to tune PID.
 */
public class PIDTest extends CommandGroup {

	/**
	 * Default constructor
	 *
	 * @param subsystem the UnidirectionalDrive to execute this command on
	 * @param driveTime How long to drive forwards for, in seconds.
	 */
	public PIDTest(UnidirectionalDrive subsystem, double driveTime) {

		//Drive forward for a bit
		addSequential(new DriveAtSpeed(subsystem, 0.7, driveTime));
		//Stop
		addSequential(new DriveAtSpeed(subsystem, 0, 100));
	}
}