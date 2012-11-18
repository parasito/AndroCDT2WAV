package es.aurdroid.cdt2wav;

public class CDT2WAVWAVOutput extends CDT2WAVBaseOutput {
	private WAVHeader wavHeader;

	public CDT2WAVWAVOutput(int freq) {
		super(freq);
	}

	public void dispose() {
		super.dispose();
		this.wavHeader = null;
	}

	protected void init() {
		this.wavHeader = new WAVHeader();
		this.wavHeader.ChunkID = 1179011410;

		this.wavHeader.ChunkSize = 0;
		this.wavHeader.Format = 1163280727;
		this.wavHeader.fmtChunkID = 544501094;
		this.wavHeader.fmtChunkSize = 16;
		this.wavHeader.AudioFormat = 1;
		this.wavHeader.SampleRate = ((int) getFrequency());
		this.wavHeader.Subchunk2ID = 1635017060;
		this.wavHeader.Subchunk2Size = 0;
		this.wavHeader.NumChannels = 1;
		this.wavHeader.BitsPerSample = 8;
		this.wavHeader.ByteRate = ((int) getFrequency());
		this.wavHeader.BlockAlign = 1;
	}

	protected void write(int numsamples) {
		this.wavHeader.Subchunk2Size = (outputTell() + numsamples - 44);
		this.wavHeader.ChunkSize = (36 + this.wavHeader.Subchunk2Size);
		byte sample;
		
		if (isNoAmp())
			sample = -128;
		else 
			sample = (byte) (isLowAmp() ? 0 : -1);
		outputByte(sample, numsamples);
	}

	protected void stop() {
		this.wavHeader.write();
	}

	public class WAVHeader {
		public int ChunkID;
		public int ChunkSize;
		public int Format;
		public int fmtChunkID;
		public int fmtChunkSize;
		public short AudioFormat;
		public short NumChannels;
		public int SampleRate;
		public int ByteRate;
		public short BlockAlign;
		public short BitsPerSample;
		public int Subchunk2ID;
		public int Subchunk2Size;

		public WAVHeader() {
		}

		private void writeInt(int data) {
			CDT2WAVWAVOutput.this.outputByte((byte) (data & 0xFF));
			CDT2WAVWAVOutput.this.outputByte((byte) (data >> 8 & 0xFF));
			CDT2WAVWAVOutput.this.outputByte((byte) (data >> 16 & 0xFF));
			CDT2WAVWAVOutput.this.outputByte((byte) (data >> 24 & 0xFF));
		}

		private void writeShort(short data) {
			CDT2WAVWAVOutput.this.outputByte((byte) (data & 0xFF));
			CDT2WAVWAVOutput.this.outputByte((byte) (data >> 8 & 0xFF));
		}

		public void write() {
			int oldPos = CDT2WAVWAVOutput.this.outputTell();
			CDT2WAVWAVOutput.this.outputSeek(0);

			writeInt(this.ChunkID);
			writeInt(this.ChunkSize);
			writeInt(this.Format);
			writeInt(this.fmtChunkID);
			writeInt(this.fmtChunkSize);
			writeShort(this.AudioFormat);
			writeShort(this.NumChannels);
			writeInt(this.SampleRate);
			writeInt(this.ByteRate);
			writeShort(this.BlockAlign);
			writeShort(this.BitsPerSample);
			writeInt(this.Subchunk2ID);
			writeInt(this.Subchunk2Size);

			CDT2WAVWAVOutput.this.outputSeek(oldPos);
		}
	}
}