/*
 * join1.c
 *
 * Simple program for testing join.  After exec-ing the child, it
 * waits for the child to exit.
 *
 * Geoff Voelker
 * 11/9/15
 */

#include "syscall.h"

int main (int argc, char *argv[])
{
    char *prog = "exit1.coff";
    int pid1, pid2,r, status = 0;

    printf ("execing %s...\n", prog);
    pid1 = exec (prog, 0, 0);
    pid2=  exec (prog,0,0);
    if (pid1 > 0) {
	printf ("...passed\n");
    } else {
	printf ("...failed (pid = %d)\n", pid1);
	exit (-1);
    }


    printf ("joining pid1 %d...\n", pid1);
    printf ("joining pid2 %d...\n", pid2);
    r = join (pid1, &status);
    if (r > 0) {
    printf ("...passed (status from child = %d)\n", status);
    } else if (r == 0) {
    printf ("...child exited with unhandled exception\n");
    exit (-1);
    } else {
    printf ("...failed (r = %d)\n", r);
    exit (-1);
    }




    exit(0);

    printf ("joining %d...\n", pid1);
    r = join (pid1, &status);
    if (r > 0) {
	printf ("...passed (status from child = %d)\n", status);
    } else if (r == 0) {
	printf ("...child exited with unhandled exception\n");
	exit (-1);
    } else {
	printf ("...failed (r = %d)\n", r);
	exit (-1);
    }

    // the return value from main is used as the status to exit
    return 0;
}
