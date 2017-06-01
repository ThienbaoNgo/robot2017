package org.usfirst.frc.team449.robot.interfaces.drive.shifting.commands;

import edu.wpi.first.wpilibj.command.Command;
import org.usfirst.frc.team449.robot.interfaces.drive.shifting.ShiftingDrive;
import org.usfirst.frc.team449.robot.util.Logger;

/**
 * Switches to a specified gear.
 */
public class SwitchToGear extends Command {

	/**
	 * The drive to execute this command on.
	 */
	private ShiftingDrive subsystem;

	/**
	 * The gear to switch to.
	 */
	private ShiftingDrive.gear switchTo;

	/**
	 * Default constructor
	 *
	 * @param subsystem The drive to execute this command on
	 * @param switchTo  The gear to switch to.
	 */
	public SwitchToGear(ShiftingDrive subsystem, ShiftingDrive.gear switchTo) {
		this.subsystem = subsystem;
		this.switchTo = switchTo;
	}

	/**
	 * Log when this command is initialized
	 */
	@Override
	protected void initialize() {
		Logger.addEvent("SwitchToGear init.", this.getClass());
	}

	/**
	 * Switch to the specified gear
	 */
	@Override
	protected void execute() {
		subsystem.setGear(switchTo);
	}

	/**
	 * Finish immediately because this is a state-change command.
	 *
	 * @return true
	 */
	@Override
	protected boolean isFinished() {
		return true;
	}

	/**
	 * Log when this command ends
	 */
	@Override
	protected void end() {
		Logger.addEvent("SwitchToGear end.", this.getClass());
	}

	/**
	 * Log when this command is interrupted.
	 */
	@Override
	protected void interrupted() {
		Logger.addEvent("SwitchToGear Interrupted!", this.getClass());
	}
}