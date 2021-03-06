package org.usfirst.frc.team449.robot.drive.unidirectional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.usfirst.frc.team449.robot.components.ShiftComponent;
import org.usfirst.frc.team449.robot.drive.shifting.DriveShiftable;
import org.usfirst.frc.team449.robot.jacksonWrappers.FPSTalon;
import org.usfirst.frc.team449.robot.jacksonWrappers.MappedAHRS;


/**
 * A drive with a cluster of any number of CANTalonSRX controlled motors on each side and a high and low gear.
 */
@JsonIdentityInfo(generator = ObjectIdGenerators.StringIdGenerator.class)
public class DriveTalonClusterShiftable extends DriveTalonCluster implements DriveShiftable {

	/**
	 * The component that controls shifting.
	 */
	@NotNull
	private final ShiftComponent shiftComponent;

	/**
	 * Whether not to override auto shifting
	 */
	private boolean overrideAutoshift;

	/**
	 * Default constructor.
	 *
	 * @param leftMaster                The master talon on the left side of the drive.
	 * @param rightMaster               The master talon on the right side of the drive.
	 * @param navX                      The NavX on this drive.
	 * @param VelScale                  The amount to scale the output to the motor by. Defaults to 1.
	 * @param shiftComponent            The component that controls shifting.
	 * @param startingOverrideAutoshift Whether to start with autoshift disabled. Defaults to false.
	 */
	@JsonCreator
	public DriveTalonClusterShiftable(@NotNull @JsonProperty(required = true) FPSTalon leftMaster,
	                                  @NotNull @JsonProperty(required = true) FPSTalon rightMaster,
	                                  @NotNull @JsonProperty(required = true) MappedAHRS navX,
	                                  @Nullable Double VelScale,
	                                  @NotNull @JsonProperty(required = true) ShiftComponent shiftComponent,
	                                  boolean startingOverrideAutoshift) {
		super(leftMaster, rightMaster, navX, VelScale);
		//Initialize stuff
		this.shiftComponent = shiftComponent;

		// Initialize shifting constants, assuming robot is stationary.
		overrideAutoshift = startingOverrideAutoshift;
	}

	/**
	 * @return true if currently overriding autoshifting, false otherwise.
	 */
	@Override
	public boolean getOverrideAutoshift() {
		return overrideAutoshift;
	}

	/**
	 * @param override Whether or not to override autoshifting.
	 */
	@Override
	public void setOverrideAutoshift(boolean override) {
		this.overrideAutoshift = override;
	}

	/**
	 * Set the output of each side of the drive.
	 *
	 * @param left  The output for the left side of the drive, from [-1, 1]
	 * @param right the output for the right side of the drive, from [-1, 1]
	 */
	@Override
	public void setOutput(double left, double right) {
		//If we're not shifting or using PID, or we're just turning in place, scale by the max speed in the current
		// gear
		if (overrideAutoshift) {
			super.setOutput(left, right);
		}
		//If we are shifting, scale by the high gear max speed to make acceleration smoother and faster.
		else {
			leftMaster.setGearScaledVelocity(left, gear.HIGH);
			rightMaster.setGearScaledVelocity(right, gear.HIGH);
		}
	}

	/**
	 * @return The gear this subsystem is currently in.
	 */
	@Override
	public int getGear() {
		return shiftComponent.getCurrentGear();
	}

	/**
	 * Shift to a specific gear.
	 *
	 * @param gear Which gear to shift to.
	 */
	@Override
	public void setGear(int gear) {
		shiftComponent.shiftToGear(gear);
	}
}
