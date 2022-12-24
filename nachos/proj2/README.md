## ReadMe

### File System Call

Zhenyu implemented the file system call. He detected the possible errors, checked the corner case and combined the corresponding routines which are already implemented in the stubFileSystem.class to implement the file system call.To pass stress tests, he used buffer zone in the implementation of write() and read().


### multiple processes & system call

Xingfei implemented the system calls exec(), join() and exit() based on the kernel mode routines, and used test examples(exec1.c, join1.c, execargh1.c, exit1.c, except1.c) to check their correctness.

### Support for multiprogramming

Zhengyang implemented support for multiprogramming by elaborating `readVirtualMemory()` and `writeVirtualMemory()` functions. He managed the mappings between virtual and physical address, which provided the discontinuous space in physical memory And  he modified `UserProcess.loadSections()` so that it allocated the `pageTable` and the number of physical pages based on the size of the address space required to load and run the user program.







