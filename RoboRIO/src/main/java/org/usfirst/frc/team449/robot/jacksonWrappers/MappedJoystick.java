package org.usfirst.frc.team449.robot.jacksonWrappers;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import edu.wpi.first.wpilibj.Joystick;

/**
 * A Jackson-compatible wrapper on a {@link Joystick}.
 */
@JsonIdentityInfo(generator = ObjectIdGenerators.StringIdGenerator.class)
public class MappedJoystick extends Joystick {

	/**
	 * Default constructor
	 *
	 * @param port The USB port of this joystick, on [0, 5].
	 */
	@JsonCreator
	public MappedJoystick(@JsonProperty(required = true) int port) {
		super(port);
	}
}
