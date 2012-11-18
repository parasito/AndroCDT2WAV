package es.aurdroid.cdt2wav;

public abstract class CDT2WAVBaseOutput {
	private static final int LOAMP = 0;
	private static final int HIAMP = 255;
	private static final int NOAMP = 128;
	private double z80_freq = 3500000.0D;
	private byte[] buf = null;
	private int bufPos = 0;
	private double frequency = 44100.0D;
	private double cycle = 0.0D;
	private int amp = 0;

	public CDT2WAVBaseOutput(int freq) {
		this.buf = null;
		this.bufPos = 0;
		this.frequency = freq;
		if (freq == 44100)
			this.z80_freq = 3400000.0D;
		else if (freq == 22050)
			this.z80_freq = 3394400.0D;
		else if (freq == 11025) {
			this.z80_freq = 3364400.0D;
		}
		this.cycle = (this.frequency / this.z80_freq);

		init();
	}

	public void dispose() {
		this.buf = null;
	}

	protected abstract void init();

	protected abstract void write(int paramInt);

	protected abstract void stop();

	protected double getFrequency() {
		return this.frequency;
	}

	protected void outputSeek(int pos) {
		this.bufPos = pos;
	}

	protected int outputTell() {
		return this.bufPos;
	}

	protected final void outputByte(byte b) {
		if (this.buf != null) {
			this.buf[this.bufPos] = b;
		}
		this.bufPos += 1;
	}

	protected final void outputByte(byte b, int count) {
		if (this.buf != null) {
			int p = this.bufPos;
			for (int i = 0; i < count; i++) {
				this.buf[(p++)] = b;
			}
		}
		this.bufPos += count;
	}

	public void setOutputBuffer(byte[] buf) {
		this.buf = buf;
		this.bufPos = 0;
	}

	public int samples(int tstates) {
		return (int) (0.5D + this.cycle * tstates);
	}

	public void setAmp(boolean high) {
		this.amp = (high ? 255 : 0);
	}

	public void setAmpLow() {
		this.amp = 0;
	}

	public void setAmpNo() {
		this.amp = 128;
	}

	public void toggleAmp() {
		if (isLowAmp())
			this.amp = 255;
		else
			this.amp = 0;
	}

	protected boolean isLowAmp() {
		return this.amp == 0;
	}

	protected boolean isNoAmp() {
		return this.amp == 128;
	}

	public void play(int numsamples) {
		write(numsamples);
	}

	public void pause(int ms) {
		int p = (int) (ms * this.frequency / 1000.0D);
		play(p);
	}
}