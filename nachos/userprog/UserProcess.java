package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;
import nachos.userprog.UserKernel.*;

import java.io.EOFException;
import java.util.*;


/**
 * Encapsulates the state of a user process that is not contained in its user
 * thread (or threads). This includes its address translation state, a file
 * table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see nachos.vm.VMProcess
 * @see nachos.network.NetProcess
 */
public class UserProcess {
	/**
	 * Allocate a new process.
	 */
	public UserProcess() {
		int numPhysPages = Machine.processor().getNumPhysPages(); //Here is 16
//		pageTable = new TranslationEntry[256];
//		for (int i = 0; i < 256; i++)
//			pageTable[i] = new TranslationEntry(i, i, false, false, false, false);

		//Xingfei
		lock1.acquire();
		globalpid++;
		numsOfProcesses++;
		lock1.release();
		cv1 = new Condition(lock2);
		this.pid=globalpid;
		fileDescriptor.put(0,stdInput);
		fileDescriptor.put(1,stdOutput);

	}

	/**
	 * Allocate and return a new process of the correct class. The class name is
	 * specified by the <tt>nachos.conf</tt> key
	 * <tt>Kernel.processClassName</tt>.
	 *
	 * @return a new process of the correct class.
	 */
	public static UserProcess newUserProcess() {
		String name = Machine.getProcessClassName ();
		// If Lib.constructObject is used, it quickly runs out
		// of file descriptors and throws an exception in
		// createClassLoader.  Hack around it by hard-coding
		// creating new processes of the appropriate type.

		if (name.equals ("nachos.userprog.UserProcess")) {
			return new UserProcess ();
		} else if (name.equals ("nachos.vm.VMProcess")) {
			return new VMProcess ();
		} else {
			return (UserProcess) Lib.constructObject(Machine.getProcessClassName());
		}
	}

	/**
	 * Execute the specified program with the specified arguments. Attempts to
	 * load the program, and then forks a thread to run it.
	 *
	 * @param name the name of the file containing the executable.
	 * @param args the arguments to pass to the executable.
	 * @return <tt>true</tt> if the program was successfully executed.
	 */
	public boolean execute(String name, String[] args) {
		if (!load(name, args))
			return false;

		thread = new UThread(this);
		thread.setName(name).fork();

		return true;
	}

	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */
	public void saveState() {
	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() {
		Machine.processor().setPageTable(pageTable);
	}

	public void handlePageFaultException(int wrongVaddr){
//        System.out.println("Page Fault Exception");
		System.out.println("WrongVaddr is"+wrongVaddr);
		System.out.println("In User Process");
		int pageFaultNum=Processor.pageFromAddress(wrongVaddr);
		System.out.println("pagefault num is "+pageFaultNum);
		Lib.assertTrue(pageFaultNum<pageTable.length);
		pageTable[pageFaultNum]=((VMKernel) Kernel.kernel).getPages(1)[0];
		pageTable[pageFaultNum].vpn=pageFaultNum;
		System.out.println("ppn is "+pageTable[pageFaultNum].ppn);

//        System.out.println(pageTable[pageFaultNum].valid);
//        System.out.println("Coff Section number is"+coff.getNumSections());
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
			Lib.debug(dbgProcess, "\tinitializing " + section.getName()
					+ " section (" + section.getLength() + " pages)");

			for (int i = 0; i < section.getLength(); i++) {
				int vpn = section.getFirstVPN() + i;
				//System.out.println("Coff vpn is"+vpn);
				if(vpn==pageFaultNum){
					section.loadPage(i,pageTable[vpn].ppn);
					pageTable[vpn].valid=true;
					return;
				}

			}
		}
		System.out.println("Stack data init");
		byte[] memory = Machine.processor().getMemory();
		int paddr = pageTable[pageFaultNum].ppn * pageSize;
		Arrays.fill(memory, paddr , paddr + pageSize, (byte) 0);
		pageTable[pageFaultNum].valid=true;
	}


	/**
	 * Read a null-terminated string from this process's virtual memory. Read at
	 * most <tt>maxLength + 1</tt> bytes from the specified address, search for
	 * the null terminator, and convert it to a <tt>java.lang.String</tt>,
	 * without including the null terminator. If no null terminator is found,
	 * returns <tt>null</tt>.
	 *
	 * @param vaddr the starting virtual address of the null-terminated string.
	 * @param maxLength the maximum number of characters in the string, not
	 * including the null terminator.
	 * @return the string read, or <tt>null</tt> if no null terminator was
	 * found.
	 */
	public String readVirtualMemoryString(int vaddr, int maxLength) {
		Lib.assertTrue(maxLength >= 0);

		byte[] bytes = new byte[maxLength + 1];

		int bytesRead = readVirtualMemory(vaddr, bytes);

		for (int length = 0; length < bytesRead; length++) {
			if (bytes[length] == 0)
				return new String(bytes, 0, length);
		}

		return null;
	}

	/**
	 * Transfer data from this process's virtual memory to all of the specified
	 * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 *
	 * @param vaddr the first byte of virtual memory to read.
	 * @param data the array where the data will be stored.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data) {
		return readVirtualMemory(vaddr, data, 0, data.length);
	}



	/**
	 * Transfer data from this process's virtual memory to the specified array.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 *
	 * @param vaddr the first byte of virtual memory to read.
	 * @param data the array where the data will be stored.
	 * @param offset the first byte to write in the array.
	 * @param length the number of bytes to transfer from virtual memory to the
	 * array.
	 * @return the number of bytes successfully transferred.
	 */



	public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
	/*	Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		byte[] memory = Machine.processor().getMemory();

		// for now, just assume that virtual addresses equal physical addresses
		if (vaddr < 0 || vaddr >= memory.length)
			return 0;

		int amount = Math.min(length, memory.length - vaddr);
		System.arraycopy(memory, vaddr, data, offset, amount);

		return amount;

	 */
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		byte[] memory = Machine.processor().getMemory();

		// for now, just assume that virtual addresses equal physical addresses
//  if (vaddr < 0 || vaddr >= memory.length)
//   return 0;
//
//  int amount = Math.min(length, memory.length - vaddr);
//  System.arraycopy(memory, vaddr, data, offset, amount);
		int amount = 0;
		while(length > 0 && offset < data.length){
			int addressOffset = vaddr % 1024;
			int vpn = vaddr / 1024;

			if(vpn >= pageTable.length || vpn < 0){
				readError = true;
				break;
			}

			TranslationEntry PTE = pageTable[vpn];
			if(!PTE.valid){
				handlePageFaultException(vaddr);
//				System.out.println(vpn);
//				System.out.println("syscall PTE vpn is "+PTE.vpn);
//				System.out.println("syscall PTE vppn is "+PTE.ppn);
				PTE=pageTable[vpn];
			}

			int physicalAddress = PTE.ppn * 1024 + addressOffset;
			int currentTransferred = Math.min(data.length - offset, Math.min(length, 1024 - addressOffset));
			System.arraycopy(memory, physicalAddress, data, offset, currentTransferred);

			vaddr += currentTransferred;
			offset += currentTransferred;
			length -= currentTransferred;
			amount += currentTransferred;

		}


		return amount;
	}




	/**
	 * Transfer all data from the specified array to this process's virtual
	 * memory. Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 *
	 * @param vaddr the first byte of virtual memory to write.
	 * @param data the array containing the data to transfer.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data) {
		return writeVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from the specified array to this process's virtual memory.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 *
	 * @param vaddr the first byte of virtual memory to write.
	 * @param data the array containing the data to transfer.
	 * @param offset the first byte to transfer from the array.
	 * @param length the number of bytes to transfer from the array to virtual
	 * memory.
	 * @return the number of bytes successfully transferred.
	 */
	/*
	public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		byte[] memory = Machine.processor().getMemory();

		// for now, just assume that virtual addresses equal physical addresses
		if (vaddr < 0 || vaddr >= memory.length)
			return 0;

		int amount = Math.min(length, memory.length - vaddr);
		System.arraycopy(data, offset, memory, vaddr, amount);

		return amount;
	}
	 */

	public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		byte[] memory = Machine.processor().getMemory();

		// for now, just assume that virtual addresses equal physical addresses
//  if (vaddr < 0 || vaddr >= memory.length)
//   return 0;
//
//  int amount = Math.min(length, memory.length - vaddr);
//  System.arraycopy(data, offset, memory, vaddr, amount);
		int amount = 0;
		while(length > 0 && offset < data.length){
			int addressOffset = vaddr % 1024;
			int vpn = vaddr / 1024;

			if(vpn >= pageTable.length || vpn < 0){
				System.out.println("error in write VirtualMemory");
				writeError = true;
				break;
			}

			TranslationEntry PTE = pageTable[vpn];
			if(!PTE.valid){
				handlePageFaultException(vaddr);
				System.out.println(vpn);
				System.out.println("syscall PTE vpn is "+PTE.vpn);
				System.out.println("syscall PTE ppn is "+PTE.ppn);
				PTE=pageTable[vpn];
			}
			if( PTE.readOnly){
				writeError = true;
				break;
			}
			PTE.dirty = true;
			System.out.println("before pin");
			System.out.println("after pin");

			int physicalAddress = PTE.ppn * 1024 + addressOffset;
			int currentTransferred = Math.min(data.length - offset, Math.min(length, 1024 - addressOffset));
			System.arraycopy(data, offset, memory, physicalAddress, currentTransferred);

			vaddr += currentTransferred;
			offset += currentTransferred;
			length -= currentTransferred;
			amount += currentTransferred;

		}
		System.out.println("finish write virtual memory");
		return amount;
	}



	/**
	 * Load the executable with the specified name into this process, and
	 * prepare to pass it the specified arguments. Opens the executable, reads
	 * its header information, and copies sections and arguments into this
	 * process's virtual memory.
	 *
	 * @param name the name of the file containing the executable.
	 * @param args the arguments to pass to the executable.
	 * @return <tt>true</tt> if the executable was successfully loaded.
	 */
	private boolean load(String name, String[] args) {
		Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");

		OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
		if (executable == null) {
			Lib.debug(dbgProcess, "\topen failed");
			return false;
		}

		try {
			coff = new Coff(executable);
		}
		catch (EOFException e) {
			executable.close();
			Lib.debug(dbgProcess, "\tcoff load failed");
			return false;
		}

		// make sure the sections are contiguous and start at page 0
		numPages = 0;
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
			if (section.getFirstVPN() != numPages) {
				coff.close();
				Lib.debug(dbgProcess, "\tfragmented executable");
				return false;
			}
			numPages += section.getLength();
		}

		// make sure the argv array will fit in one page
		byte[][] argv = new byte[args.length][];
		int argsSize = 0;
		for (int i = 0; i < args.length; i++) {
			argv[i] = args[i].getBytes();
			// 4 bytes for argv[] pointer; then string plus one for null byte
			argsSize += 4 + argv[i].length + 1;
		}
		if (argsSize > pageSize) {
			coff.close();
			Lib.debug(dbgProcess, "\targuments too long");
			return false;
		}

		// program counter initially points at the program entry point
		initialPC = coff.getEntryPoint();

		// next comes the stack; stack pointer initially points to top of it
		numPages += stackPages;
		initialSP = numPages * pageSize;

		// and finally reserve 1 page for arguments
		numPages++;

		if (!loadSections())
			return false;

		// store arguments in last page
		int entryOffset = (numPages - 1) * pageSize;
		int stringOffset = entryOffset + args.length * 4;

		this.argc = args.length;
		this.argv = entryOffset;

		for (int i = 0; i < argv.length; i++) {
			byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
			Lib.assertTrue(writeVirtualMemory(entryOffset, stringOffsetBytes) == 4);
			entryOffset += 4;
			Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) == argv[i].length);
			stringOffset += argv[i].length;
			Lib.assertTrue(writeVirtualMemory(stringOffset, new byte[] { 0 }) == 1);
			stringOffset += 1;
		}

		return true;
	}

	/**
	 * Allocates memory for this process, and loads the COFF sections into
	 * memory. If this returns successfully, the process will definitely be run
	 * (this is the last step in process initialization that can fail).
	 *
	 * @return <tt>true</tt> if the sections were successfully loaded.
	 */
	protected boolean loadSections() {
		if (numPages > Machine.processor().getNumPhysPages()) {
			coff.close();
			Lib.debug(dbgProcess, "\tinsufficient physical memory");
			return false;
		}
		System.out.println("Xingfei numPages: " + numPages);
		pageTable = ((UserKernel) Kernel.kernel).getPages(numPages);
		for(int k = 0; k < pageTable.length; k++){
			pageTable[k].vpn = k;
		}

		// load sections
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);

			Lib.debug(dbgProcess, "\tinitializing " + section.getName()
					+ " section (" + section.getLength() + " pages)");

			for (int i = 0; i < section.getLength(); i++) {
				int vpn = section.getFirstVPN() + i;

				// for now, just assume virtual addresses=physical addresses
				section.loadPage(i, pageTable[vpn].ppn);
			}
		}

		return true;
	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
		((UserKernel)Kernel.kernel).clearPages(pageTable);
	}

	/**
	 * Initialize the processor's registers in preparation for running the
	 * program loaded into this process. Set the PC register to point at the
	 * start function, set the stack pointer register to point at the top of the
	 * stack, set the A0 and A1 registers to argc and argv, respectively, and
	 * initialize all other registers to 0.
	 */
	public void initRegisters() {
		Processor processor = Machine.processor();

		// by default, everything's 0
		for (int i = 0; i < processor.numUserRegisters; i++)
			processor.writeRegister(i, 0);

		// initialize PC and SP according
		processor.writeRegister(Processor.regPC, initialPC);
		processor.writeRegister(Processor.regSP, initialSP);

		// initialize the first two argument registers to argc and argv
		processor.writeRegister(Processor.regA0, argc);
		processor.writeRegister(Processor.regA1, argv);
	}


	//Xingfei Methods
	public void reportChild(int pid, Integer status){
		SubProcess subProcess = processMap.get(pid);
		if(subProcess == null) return;
		subProcess.status = status;
		subProcess.process = null;
	}

	public boolean verifyAddr(int addr){
		if(Processor.pageFromAddress(addr) < 0 || Processor.pageFromAddress(addr) > numPages) return false;
		return true;
	}


	/**
	 * Handle the halt() system call.
	 */
	private int handleHalt() {

		Machine.halt();

		Lib.assertNotReached("Machine.halt() did not halt machine!");
		return 0;
	}

	/**
	 * Handle the exit() system call.
	 */
	private int handleExit(Integer status) {
		// Do not remove this call to the autoGrader...
		Machine.autoGrader().finishingCurrentProcess(status);
		// ...and leave it as the top of handleExit so that we
		// can grade your implementation.

		lock2.acquire();

		//getChildInfoAndUpdateStatus
		if(curProcess != null) curProcess.reportChild(pid, status);

		for(SubProcess child: processMap.values()){
			if(child.process !=null) child.process.curProcess = null;
		}
		processMap = null;

		//close all of them
		ArrayList<Integer> arrayList = new ArrayList<Integer>();
		for (Integer key : fileDescriptor.keySet()) {
			arrayList.add(key);
		}
		for(int i = 0; i < arrayList.size(); i++){
			handleClose(arrayList.get(i));
		}
		coff.close();

		//free virtual memory
		unloadSections();

		//wake up waiting processes
		flag = true;
		cv1.wakeAll();
		lock2.release();

		//Halt
		lock1.acquire();
		numsOfProcesses--;
		if (0 == numsOfProcesses) {
			Lib.debug(dbgProcess, "UserProcess.handleExit (" + status + ")");
			Kernel.kernel.terminate();
		}
		lock1.release();

		Lib.debug(dbgProcess, "UserProcess.handleExit (" + status + ")");
		// for now, unconditionally terminate with just one process
		//Kernel.kernel.terminate();
		KThread.finish();

		return 0;
	}


	private int handleExec(int name, int count, int argv) {
		//ignore corner case
		if(verifyAddr(name) == false){
			handleExit(null);
			return -1;
		}
		if(verifyAddr(argv) == false){
			handleExit(null);
			return -1;
		}

		String coff = readVirtualMemoryString(name, 256);
		//System.out.println(coff + " coff.substring(coff.length()-5) " + coff.substring(coff.length()-5));
		String postfix = coff.substring(coff.length()-5);
		//System.out.println("postfix " + postfix);
		if(!postfix.equals(".coff")) {
			//System.out.println("here postfix " + postfix);
			handleExit(null);
			return -1;
		}
		if(coff == null){
			handleExit(null);
			return -1;
		}
		//if (coff == null || !coff.endsWith(".coff")) return -1;

		String argsList[] = new String[count];
		byte array[] = new byte[4 * count];
		int len = readVirtualMemory(argv, array);
		//cannot read all
		if(4 * count != len){
			handleExit(null);
			return -1;
		}

		for(int i = 0; i < count; i++){
			// virtualMemory:
			// first part store pointer info, so we first get pointer
			// after all pointer info, it's real data
			// so use pointer to get info from VM
			int tmp = Lib.bytesToInt(array, 4 * i);
			if(verifyAddr(tmp) == false){
				handleExit(null);
				return -1;
			}
			argsList[i] = readVirtualMemoryString(tmp, 256);
		}
		UserProcess userProcess = newUserProcess();
		userProcess.curProcess = this;
		//update child-parent relationships
		processMap.put(userProcess.pid, new SubProcess(userProcess));

		userProcess.execute(coff, argsList);

		return userProcess.pid;
	}

	private int handleJoin(int pid, int status) {
		if(verifyAddr(status) == false){
			handleExit(null);
			return -1;
		}
		SubProcess subProcess = processMap.get(pid);
		if(subProcess == null) {
			handleExit(null);
			return -1;
		}
		else if (subProcess.process != null) subProcess.process.joinHelper();
		processMap.remove(pid);
		if(subProcess.status == null) return 0;
		writeVirtualMemory(status, Lib.bytesFromInt(subProcess.status));
		return 1;
	}

	public void joinHelper() {
		lock2.acquire();
		while(!flag) cv1.sleep();
		lock2.release();
	}


	//Zhenyu Add
	private int handleCreate(int name) {
		String fileName = readVirtualMemoryString(name, 256);
		if (fileName == null) return -1;
		StubFileSystem stubFileSystem= (StubFileSystem) ThreadedKernel.fileSystem;
		if(fileDescriptor.size()==maxFiles){
			return -1;
		}
		OpenFile openFile=stubFileSystem.open(fileName,true);
		if(openFile==null ){
			return -1;
		}
		int fd=-1;
		for(int i=2;i<maxFiles+2;i++){
			if(!fileDescriptor.containsKey(i)){
				fd=i;
				fileDescriptor.put(fd,openFile);
				break;
			}
		}
		return fd;
	}

	//Zhenyu Add
	private int handleOpen(int name) {
		String fileName = readVirtualMemoryString(name, 256);
		if (fileName == null) return -1;
		StubFileSystem stubFileSystem= (StubFileSystem) ThreadedKernel.fileSystem;
		if(fileDescriptor.size()==maxFiles){
			return -1;
		}
		OpenFile openFile=stubFileSystem.open(fileName,false);
		if(openFile==null){
			return -1;
		}
		int fd=-1;
		for(int i=2;i<maxFiles+2;i++){
			if(!fileDescriptor.containsKey(i)){
				fd=i;
				fileDescriptor.put(fd,openFile);
				break;
			}
		}
		return fd;
	}
	//Zhenyu Add
	private int handleRead(int fd, int buffer, int count){

		if(!fileDescriptor.containsKey(fd) || count<0){
			return -1;
		}
		OpenFile openFile=(fd==0?stdInput:fileDescriptor.get(fd));
		byte[] bytes=new byte[pageSize];
		int res=0;
		for(int i=0;i<count/pageSize+1;i++){
			int curCount=Math.min(pageSize,count-i*pageSize);
			if(curCount==0){
				break;
			}
			int bytesRead=openFile.read(bytes,0,curCount);
			if(bytesRead==-1){
				return -1;
			}
			writeError=false;
			int virtualWrite=writeVirtualMemory(buffer+i*pageSize,bytes,0,bytesRead);
			if(writeError){
				return -1;
			}
			res+=virtualWrite;
		}

		return res;
	}

	//Zhenyu Add
	private int handleWrite(int fd,int buffer, int count){

		if(!fileDescriptor.containsKey(fd) || count<0){
			return -1;
		}
		OpenFile openFile=(fd==1?stdOutput:fileDescriptor.get(fd));
		byte[] bytes=new byte[pageSize];
		int virtualRead;
		int res=0;

		for(int i=0;i<count/pageSize+1;i++){
			int curCount=Math.min(pageSize,count-i*pageSize);
			if(curCount==0){
				break;
			}
			readError=false;
			virtualRead=readVirtualMemory(buffer+i*pageSize,bytes,0,curCount);
			if(readError){
				return -1;
			}
			int bytesWrite= openFile.write(bytes,0,curCount);
			if(bytesWrite==-1){
				return -1;
			}
			res+=bytesWrite;
		}
		return res;
	}

	//Zhenyu
	private int handleClose(int fd){
		if(!fileDescriptor.containsKey(fd)){
			return -1;
		}
		OpenFile openFile=fileDescriptor.get(fd);
//        if(fd==1){
//            openFile=stdOutput;
//        }
//        else if(fd==0){
//            openFile=stdOutput;
//        }
//        else{
//            openFile=fileDescriptor.get(fd);
//        }
		openFile.close();
//        if(fd!=1 && fd!=0) {
//            fileDescriptor.remove(fd);
//        }
		fileDescriptor.remove(fd);
		return 0;
	}

	//Zhenyu
	private int handleUnlink(int name){
		String fileName = readVirtualMemoryString(name, 256);
		if (fileName == null) return -1;
		StubFileSystem stubFileSystem= (StubFileSystem) ThreadedKernel.fileSystem;
		boolean flag=stubFileSystem.remove(fileName);
		if(flag){
			return 0;
		}
		else{
			return -1;
		}
	}

	private static final int syscallHalt = 0, syscallExit = 1, syscallExec = 2,
			syscallJoin = 3, syscallCreate = 4, syscallOpen = 5,
			syscallRead = 6, syscallWrite = 7, syscallClose = 8,
			syscallUnlink = 9;

	/**
	 * Handle a syscall exception. Called by <tt>handleException()</tt>. The
	 * <i>syscall</i> argument identifies which syscall the user executed:
	 *
	 * <table>
	 * <tr>
	 * <td>syscall#</td>
	 * <td>syscall prototype</td>
	 * </tr>
	 * <tr>
	 * <td>0</td>
	 * <td><tt>void halt();</tt></td>
	 * </tr>
	 * <tr>
	 * <td>1</td>
	 * <td><tt>void exit(int status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>2</td>
	 * <td><tt>int  exec(char *name, int argc, char **argv);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>3</td>
	 * <td><tt>int  join(int pid, int *status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>4</td>
	 * <td><tt>int  creat(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>5</td>
	 * <td><tt>int  open(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>6</td>
	 * <td><tt>int  read(int fd, char *buffer, int size);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>7</td>
	 * <td><tt>int  write(int fd, char *buffer, int size);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>8</td>
	 * <td><tt>int  close(int fd);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>9</td>
	 * <td><tt>int  unlink(char *name);</tt></td>
	 * </tr>
	 * </table>
	 *
	 * @param syscall the syscall number.
	 * @param a0 the first syscall argument.
	 * @param a1 the second syscall argument.
	 * @param a2 the third syscall argument.
	 * @param a3 the fourth syscall argument.
	 * @return the value to be returned to the user.
	 */
	public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
		//System.out.println("Xingfei syscall: " + syscall);
		switch (syscall) {
			case syscallHalt:
				return handleHalt();
			case syscallExit:
				return handleExit(a0);
			case syscallExec:
				return handleExec(a0, a1, a2);
			case syscallJoin:
				return handleJoin(a0, a1);
			case syscallCreate:
				return handleCreate(a0);
			case syscallOpen:
				return handleOpen(a0);
			case syscallRead:
				return handleRead(a0,a1,a2);
			case syscallWrite:
				return handleWrite(a0,a1,a2);
			case syscallClose:
				return handleClose(a0);
			case syscallUnlink:
				return handleUnlink(a0);


			default:
				Lib.debug(dbgProcess, "Unknown syscall " + syscall);
				Lib.assertNotReached("Unknown system call!");
		}
		return 0;
	}

	/**
	 * Handle a user exception. Called by <tt>UserKernel.exceptionHandler()</tt>
	 * . The <i>cause</i> argument identifies which exception occurred; see the
	 * <tt>Processor.exceptionZZZ</tt> constants.
	 *
	 * @param cause the user exception that occurred.
	 */
	public void handleException(int cause) {
		Processor processor = Machine.processor();

		switch (cause) {
			case Processor.exceptionSyscall:
				int result = handleSyscall(processor.readRegister(Processor.regV0),
						processor.readRegister(Processor.regA0),
						processor.readRegister(Processor.regA1),
						processor.readRegister(Processor.regA2),
						processor.readRegister(Processor.regA3));
				processor.writeRegister(Processor.regV0, result);
				processor.advancePC();
				break;

			default:
				Lib.debug(dbgProcess, "Unexpected exception: "
						+ Processor.exceptionNames[cause]);
				Lib.assertNotReached("Unexpected exception");
		}
	}

	public void makeInvalid(int ppn,int vpn){
		System.out.println(pageTable[vpn].valid);
		//Lib.assertTrue(pageTable[vpn].valid && pageTable[vpn].ppn==ppn);
		pageTable[vpn].valid=false;
//		for(int i=0;i<pageTable.length;i++){
//			if(pageTable[i].valid && pageTable[i].ppn==ppn){
//				pageTable[i].valid=false;
//				System.out.println("pagetable vpn "+i+"ppn "+ppn+"is invalid now");
//				return;
//			}
//		}
	}

	/** The program being run by this process. */
	protected Coff coff;

	/** This process's page table. */
	protected TranslationEntry[] pageTable;

	/** The number of contiguous pages occupied by the program. */
	protected int numPages;

	/** The number of pages in the program's stack. */
	protected final int stackPages = 8;

	/** The thread that executes the user-level program. */
	protected UThread thread;

	private int initialPC, initialSP;

	private int argc, argv;

	private static final int pageSize = Processor.pageSize;

	private static final char dbgProcess = 'a';

	public static int globalpid = 0;

	public int pid=0;

	private static int numsOfProcesses = 0;

	private static Lock lock1 = new Lock();
	private static Lock lock2 = new Lock();

	private Condition cv1;

	public UserProcess curProcess;

	private static class SubProcess {
		SubProcess(UserProcess sub) {
			process = sub;
			status = null;
		}
		public UserProcess process;
		public Integer status;
	}

	public Hashtable<Integer, SubProcess> processMap = new Hashtable<Integer, SubProcess> ();

	private boolean flag = false;

	public Hashtable<Integer, OpenFile> fileDescriptor=new Hashtable<>();

	public OpenFile stdInput=UserKernel.console.openForReading();
	public OpenFile stdOutput=UserKernel.console.openForWriting();

	private static final int maxFiles=16;

	public boolean readError = false;

	public boolean writeError = false;
}