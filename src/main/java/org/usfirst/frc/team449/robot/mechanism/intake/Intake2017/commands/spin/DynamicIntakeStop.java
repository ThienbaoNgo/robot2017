package org.usfirst.frc.team449.robot.mechanism.intake.Intake2017.commands.spin;

import org.usfirst.frc.team449.robot.ReferencingCommand;
import org.usfirst.frc.team449.robot.mechanism.intake.Intake2017.Intake2017;

/**
 * Created by ryant on 2017-02-18.
 */
public class DynamicIntakeStop extends ReferencingCommand {
	Intake2017 intake;

	public DynamicIntakeStop(Intake2017 intake) {
		super(intake);
//		requires(intake);
		this.intake = intake;
	}

	@Override
	protected void initialize() {
		intake.setActuatedVictor(0);
	}

	@Override
	protected boolean isFinished() {
		return true;
	}

	@Override
	protected void end() {
		intake.setActuatedVictor(0);
	}

	@Override
	protected void interrupted() {
		intake.setActuatedVictor(0);
	}
}