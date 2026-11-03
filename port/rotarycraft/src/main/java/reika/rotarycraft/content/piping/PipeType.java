/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.piping;

/**
 * Original: the MachineRegistry entries of BlockPiping (HOSE, PIPE, FUELLINE, BEDPIPE, ...). The connection rules of
 * {@link PipeConnector} and of the pipes themselves are written against this type, as the original used MachineRegistry.
 */
public enum PipeType {
	HOSE,
	PIPE,
	FUEL_LINE,
	BEDROCK_PIPE,
	VALVE,
	BYPASS,
	SEPARATION,
	SUCTION;

	/** Original MachineRegistry.isStandardPipe. */
	public boolean isStandardPipe() {
		return this == PIPE || this == BEDROCK_PIPE;
	}
}
