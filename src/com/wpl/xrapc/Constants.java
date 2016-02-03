package com.wpl.xrapc;

class Constants {
	static final short SIGNATURE = (short)((0xAAA5)&0xffff);
	static final int POST_COMMAND = 1;
	static final int POST_OK_COMMAND = 2;
	static final int GET_COMMAND = 3;
	static final int GET_OK_COMMAND = 4;
	static final int GET_EMPTY_COMMAND = 5;
	static final int PUT_COMMAND = 6;
	static final int PUT_OK_COMMAND = 7;
	static final int DELETE_COMMAND = 8;
	static final int DELETE_OK_COMMAND = 9;
	static final int ERROR_COMMAND = 10;
}
