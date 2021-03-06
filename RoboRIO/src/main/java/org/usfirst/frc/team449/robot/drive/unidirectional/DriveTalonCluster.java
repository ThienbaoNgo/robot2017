package org.usfirst.frc.team449.robot.drive.unidirectional;

import com.fasterxml.jackson.annotation.*;
import com.kauailabs.navx.frc.AHRS;
import edu.wpi.first.wpilibj.command.Command;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.usfirst.frc.team449.robot.generalInterfaces.loggable.Loggable;
import org.usfirst.frc.team449.robot.jacksonWrappers.FPSTalon;
import org.usfirst.frc.team449.robot.jacksonWrappers.MappedAHRS;
import org.usfirst.frc.team449.robot.jacksonWrappers.YamlSubsystem;
import org.usfirst.frc.team449.robot.other.MotionProfileData;
import org.usfirst.frc.team449.robot.subsystem.interfaces.motionProfile.TwoSideMPSubsystem.SubsystemMPTwoSides;
import org.usfirst.frc.team449.robot.subsystem.interfaces.navX.SubsystemNavX;


/**
 * A drive with a cluster of any number of CANTalonSRX controlled motors on each side.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.WRAPPER_OBJECT, property = "@class")
@JsonIdentityInfo(generator = ObjectIdGenerators.StringIdGenerator.class)
public class DriveTalonCluster extends YamlSubsystem implements SubsystemNavX, DriveUnidirectional, Loggable, SubsystemMPTwoSides {

	/**
	 * Joystick scaling constant. Joystick output is scaled by this before being handed to the motors.
	 */
	protected final double VEL_SCALE;

	/**
	 * Right master Talon
	 */
	@NotNull
	protected final FPSTalon rightMaster;

	/**
	 * Left master Talon
	 */
	@NotNull
	protected final FPSTalon leftMaster;

	/**
	 * The NavX gyro
	 */
	@NotNull
	private final AHRS navX;

	/**
	 * Whether or not to use the NavX for driving straight
	 */
	private boolean overrideNavX;

	/**
	 * Default constructor.
	 *
	 * @param leftMaster  The master talon on the left side of the drive.
	 * @param rightMaster The master talon on the right side of the drive.
	 * @param navX        The NavX gyro for calculating this drive's heading and angular velocity.
	 * @param VelScale    The amount to scale the output to the motor by. Defaults to 1.
	 */
	@JsonCreator
	public DriveTalonCluster(@NotNull @JsonProperty(required = true) FPSTalon leftMaster,
	                         @NotNull @JsonProperty(required = true) FPSTalon rightMaster,
	                         @NotNull @JsonProperty(required = true) MappedAHRS navX,
	                         @Nullable Double VelScale) {
		super();
		//Initialize stuff
		this.VEL_SCALE = VelScale != null ? VelScale : 1.;
		this.rightMaster = rightMaster;
		this.leftMaster = leftMaster;
		this.navX = navX;
	}

	/**
	 * Set the output of each side of the drive.
	 *
	 * @param left  The output for the left side of the drive, from [-1, 1]
	 * @param right the output for the right side of the drive, from [-1, 1]
	 */
	@Override
	public void setOutput(double left, double right) {
		//scale by the max speed
		leftMaster.setVelocity(VEL_SCALE * left);
		rightMaster.setVelocity(VEL_SCALE * right);
	}

	/**
	 * Get the velocity of the left side of the drive.
	 *
	 * @return The signed velocity in rotations per second, or null if the drive doesn't have encoders.
	 */
	@Override
	@Nullable
	public Double getLeftVel() {
		return leftMaster.getVelocity();
	}

	/**
	 * Get the velocity of the right side of the drive.
	 *
	 * @return The signed velocity in rotations per second, or null if the drive doesn't have encoders.
	 */
	@Override
	@Nullable
	public Double getRightVel() {
		return rightMaster.getVelocity();
	}

	/**
	 * Completely stop the robot by setting the voltage to each side to be 0.
	 */
	@Override
	public void fullStop() {
		leftMaster.setPercentVoltage(0);
		rightMaster.setPercentVoltage(0);
	}

	/**
	 * If this drive uses motors that can be disabled, enable them.
	 */
	@Override
	public void enableMotors() {
		leftMaster.enable();
		rightMaster.enable();
	}

	/**
	 * Stuff run on first enable.
	 */
	@Override
	protected void initDefaultCommand() {
		//Do nothing, the default command gets set with setDefaultCommandManual
	}

	/**
	 * Set the default command. Done here instead of in initDefaultCommand so we don't have a defaultCommand during
	 * auto.
	 *
	 * @param defaultCommand The command to have run by default. Must require this subsystem.
	 */
	public void setDefaultCommandManual(Command defaultCommand) {
		setDefaultCommand(defaultCommand);
	}

	/**
	 * Get the robot's heading using the navX
	 *
	 * @return robot heading, in degrees, on [-180, 180]
	 */
	@Override
	public double getGyroOutput() {
		return navX.pidGet();
	}

	/**
	 * @return true if the NavX is currently overriden, false otherwise.
	 */
	@Override
	public boolean getOverrideNavX() {
		return overrideNavX;
	}

	/**
	 * @param override true to override the NavX, false to un-override it.
	 */
	@Override
	public void setOverrideNavX(boolean override) {
		overrideNavX = override;
	}

	/**
	 * @return An AHRS object representing this subsystem's NavX.
	 */
	@Override
	@NotNull
	public AHRS getNavX() {
		return navX;
	}

	/**
	 * Get the headers for the data this subsystem logs every loop.
	 *
	 * @return An N-length array of String labels for data, where N is the length of the Object[] returned by getData().
	 */
	@Override
	@NotNull
	@Contract(pure = true)
	public String[] getHeader() {
		return new String[]{"left_vel",
				"right_vel",
				"left_setpoint",
				"right_setpoint",
				"left_current",
				"right_current",
				"left_voltage",
				"right_voltage",
				"left_pos",
				"right_pos",
				"raw_angle"};
	}

	/**
	 * Get the data this subsystem logs every loop.
	 *
	 * @return An N-length array of Objects, where N is the number of labels given by getHeader.
	 */
	@Override
	@NotNull
	public Object[] getData() {
		return new Object[]{leftMaster.getVelocity(),
				rightMaster.getVelocity(),
				leftMaster.getSetpoint(),
				rightMaster.getSetpoint(),
				leftMaster.getOutputCurrent(),
				rightMaster.getOutputCurrent(),
				leftMaster.getOutputVoltage(),
				rightMaster.getOutputVoltage(),
				leftMaster.getPositionFeet(),
				rightMaster.getPositionFeet(),
				navX.getAngle()};
	}

	/**
	 * Get the name of this object.
	 *
	 * @return A string that will identify this object in the log file.
	 */
	@Override
	@NotNull
	@Contract(pure = true)
	public String getName() {
		return "Drive";
	}

	/**
	 * Loads a profile into the MP buffer.
	 *
	 * @param profile The profile to be loaded.
	 */
	@Override
	public void loadMotionProfile(@NotNull MotionProfileData profile) {
		leftMaster.loadProfile(profile);
		rightMaster.loadProfile(profile);
	}

	/**
	 * Loads given profiles into the left and right sides of the drive.
	 *
	 * @param left  The profile to load into the left side.
	 * @param right The profile to load into the right side.
	 */
	@Override
	public void loadMotionProfile(@NotNull MotionProfileData left, @NotNull MotionProfileData right) {
		leftMaster.loadProfile(left);
		rightMaster.loadProfile(right);
	}

	/**
	 * Start running the profile that's currently loaded into the MP buffer.
	 */
	@Override
	public void startRunningLoadedProfile() {
		leftMaster.startRunningMP();
		rightMaster.startRunningMP();
	}

	/**
	 * Get whether this subsystem has finished running the profile loaded in it.
	 *
	 * @return true if there's no profile loaded and no profile running, false otherwise.
	 */
	@Override
	public boolean profileFinished() {
		return leftMaster.MPIsFinished() && rightMaster.MPIsFinished();
	}

	/**
	 * Disable the motors.
	 */
	@Override
	public void disable() {
		leftMaster.disable();
		rightMaster.disable();
	}

	/**
	 * Hold the current position.
	 */
	@Override
	public void holdPosition() {
		leftMaster.holdPositionMP();
		rightMaster.holdPositionMP();
	}

	/**
	 * Get whether the subsystem is ready to run the loaded profile.
	 *
	 * @return true if a profile is loaded and ready to run, false otherwise.
	 */
	@Override
	public boolean readyToRunProfile() {
		return leftMaster.readyForMP() && rightMaster.readyForMP();
	}

	/**
	 * Stops any MP-related threads currently running. Normally called at the start of teleop.
	 */
	@Override
	public void stopMPProcesses() {
		leftMaster.stopMPProcesses();
		rightMaster.stopMPProcesses();
	}

	/**
	 * Reset the motor positions.
	 */
	public void resetPosition() {
		leftMaster.resetPosition();
		rightMaster.resetPosition();
	}
}
