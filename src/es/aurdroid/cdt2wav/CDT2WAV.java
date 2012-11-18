package es.aurdroid.cdt2wav;

public class CDT2WAV {
	private static byte[] ZXTAPE_HEADER = { 90, 88, 84, 97, 112, 101, 33 };

	protected boolean noamp = false;

	protected boolean debug = false;
	private byte[] inpbuf;
	private int frequency = 44100;
	private CDT2WAVBaseOutput output;
	private int currentBlock;
	private int numBlocks;
	private int[] blockStart;
	public String[] ids;
	public int[] blocks;
	private int id;
	private int data;
	private int datalen;
	private int datapos;
	private int bitcount;
	private int sb_bit;
	private byte databyte;
	private int pilot;
	private int sb_pilot;
	private int sb_sync1;
	private int sb_sync2;
	private int sb_bit0;
	private int sb_bit1;
	private int sb_pulse;
	private int lastbyte;
	private int pause;
	private int singlepulse;
	private short jump;
	private int loop_start = 0;
	private int loop_count = 0;
	private int call_pos = 0;
	private int call_num = 0;
	private int call_cur = 0;

	public CDT2WAV(byte[] input, int freq, boolean useamp) {
		this.noamp = useamp;
		this.inpbuf = input;
		this.frequency = freq;
	}

	public void dispose() {
		this.inpbuf = null;
		this.blockStart = null;

		if (this.output != null) {
			this.output.dispose();
		}
		this.output = null;
	}

	private void analyseID10() {
		this.pause = get2(this.inpbuf, this.data);
		this.datalen = get2(this.inpbuf, this.data + 2);
		this.data += 4;
		if (this.inpbuf[this.data] == 0)
			this.pilot = 8064;
		else {
			this.pilot = 3220;
		}
		this.sb_pilot = this.output.samples(2168);
		this.sb_sync1 = this.output.samples(667);
		this.sb_sync2 = this.output.samples(735);
		this.sb_bit0 = this.output.samples(885);
		this.sb_bit1 = this.output.samples(1710);
		this.lastbyte = 8;
	}

	private void analyseID11() {
		this.sb_pilot = this.output.samples(get2(this.inpbuf, this.data + 0));
		this.sb_sync1 = this.output.samples(get2(this.inpbuf, this.data + 2));
		this.sb_sync2 = this.output.samples(get2(this.inpbuf, this.data + 4));
		this.sb_bit0 = this.output.samples(get2(this.inpbuf, this.data + 6));
		this.sb_bit1 = this.output.samples(get2(this.inpbuf, this.data + 8));
		this.pilot = get2(this.inpbuf, this.data + 10);
		this.lastbyte = this.inpbuf[(this.data + 12)];
		this.pause = get2(this.inpbuf, this.data + 13);
		this.datalen = get3(this.inpbuf, this.data + 15);
		this.data += 18;
		if (this.debug)
			System.out.println("Pilot is: " + this.pilot + " pause is: "
					+ this.pause + " Length is: " + this.datalen);
	}

	private void analyseID12() {
		this.sb_pilot = this.output.samples(get2(this.inpbuf, this.data + 0));
		this.pilot = get2(this.inpbuf, this.data + 2);
		while (this.pilot > 0) {
			this.output.play(this.sb_pilot);
			this.output.toggleAmp();
			this.pilot -= 1;
		}
	}

	private void analyseID13() {
		this.pilot = this.inpbuf[(this.data + 0)];
		this.data += 1;
		while (this.pilot > 0) {
			this.sb_pulse = this.output
					.samples(get2(this.inpbuf, this.data + 0));
			this.output.play(this.sb_pulse);
			this.output.toggleAmp();
			this.pilot -= 1;
			this.data += 2;
		}
	}

	private void analyseID14() {
		this.sb_pilot = (this.pilot = this.sb_sync1 = this.sb_sync2 = 0);
		this.sb_bit0 = this.output.samples(get2(this.inpbuf, this.data + 0));
		this.sb_bit1 = this.output.samples(get2(this.inpbuf, this.data + 2));
		this.lastbyte = this.inpbuf[(this.data + 4)];
		this.pause = get2(this.inpbuf, this.data + 5);
		this.datalen = get3(this.inpbuf, this.data + 7);
		this.data += 10;
	}

	private void analyseID15() {
		this.sb_pulse = this.output.samples(get2(this.inpbuf, this.data + 0));
		if (this.sb_pulse == 0) {
			this.sb_pulse = 1;
		}
		this.pause = get2(this.inpbuf, this.data + 2);
		this.lastbyte = this.inpbuf[(this.data + 4)];
		this.datalen = get3(this.inpbuf, this.data + 5);

		this.data += 8;
		this.datapos = 0;

		while (this.datalen > 0) {
			if (this.datalen != 1)
				this.bitcount = 8;
			else {
				this.bitcount = this.lastbyte;
			}
			this.databyte = this.inpbuf[(this.data + this.datapos)];
			while (this.bitcount > 0) {
				this.output.setAmp((this.databyte & 0x80) != 0);
				this.output.play(this.sb_pulse);
				this.databyte = ((byte) (this.databyte << 1));
				this.bitcount -= 1;
			}
			this.datalen -= 1;
			this.datapos += 1;
		}
		this.output.toggleAmp();
		if (this.pause != 0)
			this.output.pause(this.pause);
	}

	private void analyseID20() {
		this.pause = get2(this.inpbuf, this.data + 0);
		if (this.noamp)
			this.output.setAmpNo();
		else {
			this.output.setAmpLow();
		}
		if (this.debug) {
			System.out.println("Pause is " + this.pause);
		}
		if (this.pause != 0) {
			this.output.pause(this.pause);
		} else {
			this.output.pause(5000);
			System.out.println("Pause is added: 5 secs");
		}
		this.output.setAmpLow();
	}

	private void analyseID21() {
	}

	private void analyseID22() {
	}

	private void analyseID23() {
		this.jump = ((short) (this.inpbuf[(this.data + 0)] + this.inpbuf[(this.data + 1)] * 256));
		this.currentBlock += this.jump;
		this.currentBlock -= 1;
	}

	private void analyseID24() {
		this.loop_start = this.currentBlock;
		this.loop_count = get2(this.inpbuf, this.data + 0);
	}

	private void analyseID25() {
		this.loop_count -= 1;
		if (this.loop_count > 0)
			this.currentBlock = this.loop_start;
	}

	private void analyseID26() {
		this.call_pos = this.currentBlock;
		this.call_num = get2(this.inpbuf, this.data + 0);
		this.call_cur = 0;
		this.jump = ((short) (this.inpbuf[(this.data + 2)] + this.inpbuf[(this.data + 3)] * 256));
		this.currentBlock += this.jump;
		this.currentBlock -= 1;
	}

	private void analyseID27() {
		this.call_cur += 1;
		if (this.call_cur == this.call_num) {
			this.currentBlock = this.call_pos;
		} else {
			this.currentBlock = this.call_pos;
			this.data = (this.blockStart[this.currentBlock] + 1);
			this.jump = ((short) (this.inpbuf[(this.data + this.call_cur * 2 + 2)] + this.inpbuf[(this.data
					+ this.call_cur * 2 + 3)] * 256));
			this.currentBlock += this.jump;
			this.currentBlock -= 1;
		}
	}

	private void analyseID2A() {
		this.output.pause(5000);
		this.output.setAmpLow();
	}

	private void analyseID33() {
	}

	private static int get2(byte[] data, int pos) {
		return data[pos] & 0xFF | data[(pos + 1)] << 8 & 0xFF00;
	}

	private static int get3(byte[] data, int pos) {
		return data[pos] & 0xFF | data[(pos + 1)] << 8 & 0xFF00
				| data[(pos + 2)] << 16 & 0xFF0000;
	}

	private static int get4(byte[] data, int pos) {
		return data[pos] & 0xFF | data[(pos + 1)] << 8 & 0xFF00
				| data[(pos + 2)] << 16 & 0xFF0000 | data[(pos + 3)] << 24
				& 0xFF000000;
	}

	private int countBlocks(int[] blockstarts) {
		int pos = 10;
		int numblocks = 0;

		while (pos < this.inpbuf.length) {
			if (blockstarts != null) {
				blockstarts[numblocks] = pos;
			}
			pos++;

			switch (this.inpbuf[(pos - 1)]) {
			case 16:
				pos += get2(this.inpbuf, pos + 2) + 4;
				break;
			case 17:
				pos += get3(this.inpbuf, pos + 15) + 18;
				break;
			case 18:
				pos += 4;
				break;
			case 19:
				pos += this.inpbuf[(pos + 0)] * 2 + 1;
				break;
			case 20:
				pos += get3(this.inpbuf, pos + 7) + 10;
				break;
			case 21:
				pos += get3(this.inpbuf, pos + 5) + 8;
				break;
			case 22:
				pos += get4(this.inpbuf, pos + 0) + 4;
				break;
			case 23:
				pos += get4(this.inpbuf, pos + 0) + 4;
				break;
			case 32:
				pos += 2;
				break;
			case 33:
				pos += this.inpbuf[(pos + 0)] + 1;
				break;
			case 34:
				break;
			case 35:
				pos += 2;
				break;
			case 36:
				pos += 2;
				break;
			case 37:
				break;
			case 38:
				pos += get2(this.inpbuf, pos + 0) * 2 + 2;
				break;
			case 39:
				break;
			case 40:
				pos += get2(this.inpbuf, pos + 0) + 2;
				break;
			case 42:
				pos += 4;
				break;
			case 48:
				pos += this.inpbuf[(pos + 0)] + 1;
				break;
			case 49:
				pos += this.inpbuf[(pos + 1)] + 2;
				break;
			case 50:
				pos += get2(this.inpbuf, pos + 0) + 2;
				break;
			case 51:
				pos += this.inpbuf[(pos + 0)] * 3 + 1;
				break;
			case 52:
				pos += 8;
				break;
			case 53:
				pos += get4(this.inpbuf, pos + 16) + 20;
				break;
			case 64:
				pos += get3(this.inpbuf, pos + 1) + 4;
				break;
			case 90:
				pos += 9;
				break;
			case 24:
			case 25:
			case 26:
			case 27:
			case 28:
			case 29:
			case 30:
			case 31:
			case 41:
			case 43:
			case 44:
			case 45:
			case 46:
			case 47:
			case 54:
			case 55:
			case 56:
			case 57:
			case 58:
			case 59:
			case 60:
			case 61:
			case 62:
			case 63:
			case 65:
			case 66:
			case 67:
			case 68:
			case 69:
			case 70:
			case 71:
			case 72:
			case 73:
			case 74:
			case 75:
			case 76:
			case 77:
			case 78:
			case 79:
			case 80:
			case 81:
			case 82:
			case 83:
			case 84:
			case 85:
			case 86:
			case 87:
			case 88:
			case 89:
			default:
				return -1;
			}

			numblocks++;
		}

		return numblocks;
	}

	private void convertPass(CDT2WAVBaseOutput output) {
		this.currentBlock = 0;
		this.singlepulse = 0;

		if (output == null) {
			this.debug = false;
		}

		while (this.currentBlock < this.numBlocks) {
			this.id = this.inpbuf[this.blockStart[this.currentBlock]];
			this.blocks[this.currentBlock] = output.outputTell();
			this.ids[this.currentBlock] = getID(this.id);
			if (this.debug) {
				System.out.println("Block: " + getBlock(this.currentBlock)
						+ " - ID: " + getID(this.currentBlock));
			}
			if (this.debug) {
				System.out.println("ID is " + hex(this.id));
			}
			this.data = (this.blockStart[this.currentBlock] + 1);

			switch (this.id) {
			case 16:
				analyseID10();
				break;
			case 17:
				analyseID11();
				break;
			case 18:
				analyseID12();
				break;
			case 19:
				analyseID13();
				break;
			case 20:
				analyseID14();
				break;
			case 21:
				analyseID15();
				break;
			case 32:
				analyseID20();
				break;
			case 33:
				analyseID21();
				break;
			case 34:
				analyseID22();
				break;
			case 35:
				analyseID23();
				break;
			case 36:
				analyseID24();
				break;
			case 37:
				analyseID25();
				break;
			case 38:
				analyseID26();
				break;
			case 39:
				analyseID27();
				break;
			case 42:
				analyseID2A();
				break;
			case 51:
				analyseID33();
				break;
			case 48:
			case 49:
			case 50:
			case 52:
			case 53:
			case 64:
			case 90:
				break;
			case 22:
			case 23:
			case 24:
			case 25:
			case 26:
			case 27:
			case 28:
			case 29:
			case 30:
			case 31:
			case 40:
			case 41:
			case 43:
			case 44:
			case 45:
			case 46:
			case 47:
			case 54:
			case 55:
			case 56:
			case 57:
			case 58:
			case 59:
			case 60:
			case 61:
			case 62:
			case 63:
			case 65:
			case 66:
			case 67:
			case 68:
			case 69:
			case 70:
			case 71:
			case 72:
			case 73:
			case 74:
			case 75:
			case 76:
			case 77:
			case 78:
			case 79:
			case 80:
			case 81:
			case 82:
			case 83:
			case 84:
			case 85:
			case 86:
			case 87:
			case 88:
			case 89:
			default:
				System.out.println("ERR_TZX_UNSUPPORTED");
			}

			if ((this.id == 16) || (this.id == 17) || (this.id == 20)) {
				while (this.pilot > 0) {
					output.play(this.sb_pilot);
					output.toggleAmp();
					this.pilot -= 1;
				}

				if (this.sb_sync1 > 0) {
					output.play(this.sb_sync1);
					output.toggleAmp();
				}

				if (this.sb_sync2 > 0) {
					output.play(this.sb_sync2);
					output.toggleAmp();
				}

				this.datapos = 0;
				while (this.datalen > 0) {
					if (this.datalen != 1)
						this.bitcount = 8;
					else {
						this.bitcount = this.lastbyte;
					}

					this.databyte = this.inpbuf[(this.data + this.datapos)];

					while (this.bitcount > 0) {
						if ((this.databyte & 0x80) != 0)
							this.sb_bit = this.sb_bit1;
						else {
							this.sb_bit = this.sb_bit0;
						}
						output.play(this.sb_bit);
						output.toggleAmp();
						if (this.singlepulse == 0) {
							output.play(this.sb_bit);
							output.toggleAmp();
						}
						this.databyte = ((byte) (this.databyte << 1));
						this.bitcount -= 1;
					}
					this.datalen -= 1;
					this.datapos += 1;
				}
				this.singlepulse = 0;

				if (this.pause > 0) {
					output.pause(1);
					if (this.noamp)
						output.setAmpNo();
					else {
						output.setAmpLow();
					}
					if (this.pause > 1) {
						output.pause(this.pause - 1);
					}
				}

			}

			this.currentBlock += 1;
		}

		output.pause(5000);
		if (this.debug) {
			System.out.println("End of tape... 5 seconds pause added");
		}

		output.stop();

		if (this.debug)
			System.out.println(" OK");
	}

	public byte[] convert() {
		if ((this.inpbuf == null) || (this.inpbuf.length < 10)) {
			System.out.println("ERR_ILLEGAL_ARGUMENT");
			return null;
		}

		for (int i = 0; i < ZXTAPE_HEADER.length; i++) {
			if (this.inpbuf[i] != ZXTAPE_HEADER[i]) {
				System.out.println("ERR_NOT_TZX");
				return null;
			}

		}

		int cdt_major = this.inpbuf[8];
		if (cdt_major == 0) {
			System.out.println("ERR_TZX_UNSUPPORTED");
			return null;
		}

		this.currentBlock = 0;
		this.numBlocks = countBlocks(null);
		if (this.numBlocks < 0) {
			System.out.println("ERR_TZX_UNSUPPORTED");
			return null;
		}

		this.blockStart = new int[this.numBlocks];
		this.ids = new String[this.numBlocks + 1];
		this.blocks = new int[this.numBlocks + 1];
		countBlocks(this.blockStart);

		this.output = new CDT2WAVWAVOutput(this.frequency);

		this.debug = false;
		convertPass(this.output);

		int dataLength = this.output.outputTell();
		byte[] data = null;
		if (dataLength > 0) {
			data = new byte[dataLength];
			this.output.setOutputBuffer(data);
			convertPass(this.output);
		}

		return data;
	}

	public int getBlock(int id) {
		if (this.blocks != null) {
			return this.blocks[id];
		}
		return 0;
	}

	public String getID(int id) {
		String ret = null;

		switch (id) {
		case 16:
			ret = "Turbo data II";
			break;
		case 17:
			ret = "Turbo data";
			break;
		case 18:
			ret = "Pure tone";
			break;
		case 19:
			ret = "Sequence of pulses";
			break;
		case 20:
			ret = "Pure Data";
			break;
		case 21:
			ret = "Direct recording";
			break;
		case 32:
			ret = "Pause";
			break;
		case 33:
			ret = "Group Start";
			break;
		case 34:
			ret = "Group End";
			break;
		case 35:
			ret = "Jump relative";
			break;
		case 36:
			ret = "Loop Start";
			break;
		case 37:
			ret = "Loop End";
			break;
		case 38:
			ret = "Call Sequence";
			break;
		case 39:
			ret = "Return from Sequence";
			break;
		case 42:
			ret = "Stop tape";
			break;
		case 51:
			ret = "Hardware Info";
			break;
		case 22:
		case 23:
		case 24:
		case 25:
		case 26:
		case 27:
		case 28:
		case 29:
		case 30:
		case 31:
		case 40:
		case 41:
		case 43:
		case 44:
		case 45:
		case 46:
		case 47:
		case 48:
		case 49:
		case 50:
		default:
			ret = "Unknown block";
		}

		return ret;
	}
	
	public static String hex(byte value) {
		return "" + "0123456789ABCDEF".charAt((value & 0xF0) >> 4)
				+ "0123456789ABCDEF".charAt(value & 0xF);
	}

	public static String hex(short value) {
		return hex((byte) (value >> 8)) + hex((byte) value);
	}

	public static String hex(int value) {
		return hex((short) (value >> 16)) + hex((short) value);
	}

}