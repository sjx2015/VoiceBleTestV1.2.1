#ifndef _IMA_DEC_H_
#define _IMA_DEC_H_


extern "C" void ima_adpcm_decode_init();

extern "C" int ima_adpcm_decode_proc(short *synth, unsigned char *serial, int len);


#endif