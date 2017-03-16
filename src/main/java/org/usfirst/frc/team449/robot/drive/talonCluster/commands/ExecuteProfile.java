package org.usfirst.frc.team449.robot.drive.talonCluster.commands;

import com.ctre.CANTalon;
import edu.wpi.first.wpilibj.command.Command;
import edu.wpi.first.wpilibj.command.Subsystem;
import org.usfirst.frc.team449.robot.Robot;

import java.util.Collection;

/**
 * ReferencingCommand to load and execute a motion profile on the master Talons in the two motor clusters
 */
public class ExecuteProfile extends Command {
	//TODO Externalize all this shit
	/**
	 * Number of points that must be loaded to the bottom level buffer before we start executing the profile
	 */
	private static final int MIN_NUM_POINTS_IN_BTM = 128; // maximum number of points

	private boolean bottomLoaded;

	private Collection<CANTalon> talons;

	private boolean finished;

	/**
	 * Construct a new ExecuteProfile command
	 */
	public ExecuteProfile(Collection<CANTalon> talons, Subsystem toRequire) {
		if (toRequire != null){
			requires(toRequire);
		}
		this.talons = talons;

		finished = false;
		bottomLoaded = false;
	}

	public ExecuteProfile(Collection<CANTalon> talons){
		this(talons, null);
	}

	/**
	 * Set up the Talons' modes and populate the trajectory point buffer
	 */
	@Override
	protected void initialize() {
		for (CANTalon talon : talons) {
			talon.changeControlMode(CANTalon.TalonControlMode.MotionProfile);
			talon.set(CANTalon.SetValueMotionProfile.Disable.value);
			talon.clearMotionProfileHasUnderrun();
		}

		finished = false;
		bottomLoaded = false;
	}

	/**
	 * If its the first execute call, start the thread. Otherwise, error check every loop call. Note that the real
	 * logic is executed in the control method for black-magic Scheduler timing reasons.
	 */
	@Override
	protected void execute() {
		finished = true;
		boolean bottomNowLoaded = true;
		for (CANTalon talon : talons) {
			CANTalon.MotionProfileStatus MPStatus = new CANTalon.MotionProfileStatus();
			talon.getMotionProfileStatus(MPStatus);
			if (!bottomLoaded) {
				bottomNowLoaded = bottomNowLoaded && (MPStatus.btmBufferCnt > MIN_NUM_POINTS_IN_BTM);
			}
			if (bottomLoaded) {
				finished = finished && MPStatus.activePoint.isLastPoint;
			}
		}
		if (bottomNowLoaded && !bottomLoaded) {
			bottomLoaded = true;
			for (CANTalon talon : talons) {
				talon.enable();
				talon.set(CANTalon.SetValueMotionProfile.Enable.value);
			}
		}
	}

	@Override
	protected boolean isFinished() {
		return finished;
	}

	@Override
	protected void end() {
		for (CANTalon talon : talons) {
			talon.set(CANTalon.SetValueMotionProfile.Hold.value);
		}
		System.out.println("ExecuteProfile end.");
	}

	@Override
	protected void interrupted() {
		for (CANTalon talon : talons) {
			talon.set(CANTalon.SetValueMotionProfile.Disable.value);
		}
		System.out.println("ExecuteProfile interrupted!");
	}
}
