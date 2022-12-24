#include "syscall.h"

int main (int argc, char *argv[])
{
    char *prog = "write10.coff";
    char *prog2 = "write11.coff";
    int pid;

    pid = exec (prog, 0, 0);
    if (pid < 0) {
	exit (-1);
    }

    pid = exec (prog2, 0, 0);

    if (pid < 0) {
    	exit (-1);
        }



    printf("\n\n\n\n\n\n\n\n\nfinish\n\n\n\n\n\n\n\n");

    exit (20);
}