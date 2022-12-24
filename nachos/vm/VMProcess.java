package nachos.vm;

import nachos.machine.*;
import nachos.threads.Lock;
import nachos.userprog.*;

import java.util.*;

/**
 * A <tt>UserProcess</tt> that supports demand-paging.
 */
public class VMProcess extends UserProcess {
    /**
     * Allocate a new process.
     */
    public VMProcess() {
        super();

    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
        super.saveState();
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
        super.restoreState();
    }

    /**
     * Initializes page tables for this process so that the executable can be
     * demand-paged.
     *
     * @return <tt>true</tt> if successful.
     */


    protected boolean loadSections() {
//        if (numPages > Machine.processor().getNumPhysPages()) {
//            coff.close();
//            Lib.debug(dbgProcess, "\tinsufficient physical memory");
//            return false;
//        }
        System.out.println("VM Process numPages: " + numPages);
        //Zhenyu add: here we does not even allocate a physical page. Instead, merely mark all the TranslationEntries as invalid.
        //pageTable = ((VMKernel) Kernel.kernel).getPages(numPages);
        pageTable=new TranslationEntry[numPages];
        for (int k = 0; k < pageTable.length; k++) {
            pageTable[k]=new TranslationEntry();
            pageTable[k].vpn=k;
            pageTable[k].ppn=k;
            pageTable[k].valid = false;
            pageTable[k].readOnly=false;
            pageTable[k].used=false;
            pageTable[k].dirty=false;
        }

        // zhenyu comment this for part1
//		for (int s = 0; s < coff.getNumSections(); s++) {
//			CoffSection section = coff.getSection(s);
//
//			Lib.debug(dbgProcess, "\tinitializing " + section.getName()
//					+ " section (" + section.getLength() + " pages)");
//
//			for (int i = 0; i < section.getLength(); i++) {
//				int vpn = section.getFirstVPN() + i;
//
//				// for now, just assume virtual addresses=physical addresses
//				section.loadPage(i, pageTable[vpn].ppn);
//			}
//		}

        return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
        VMKernel.handlePageFaultLock.acquire();
        HashSet<Integer> ppnList=new HashSet<>();
        for(TranslationEntry tmp:pageTable){
            if(tmp.valid){
                tmp.valid=false;
                tmp.dirty=false;
                tmp.used=false;
                tmp.readOnly=false;
                ((VMKernel)Kernel.kernel).pages.add(new TranslationEntry(tmp));
                ppnList.add(tmp.ppn);
            }
        }
        Iterator<TranslationEntry> iterator=((VMKernel)Kernel.kernel).usedPages.iterator();
        while(iterator.hasNext()){
            TranslationEntry usedPage= iterator.next();
            int ppn=usedPage.ppn;
            if (ppnList.contains(ppn)){
                iterator.remove();
                VMKernel.TEtable.remove(usedPage);
            }
        }
        VMKernel.pointer=0;
        VMKernel.handlePageFaultLock.release();
        return;
    }

    public void swapInPage(int vpn,int ppn){
        int spn=pageTable[vpn].vpn;
        VMKernel.swapFile.read( spn* pageSize, Machine.processor().getMemory(), ppn*pageSize, pageSize);
        VMKernel.freeSpn.add(spn);
    }


    public void handlePageFaultException(int wrongVaddr){
//        System.out.println("Page Fault Exception");
        VMKernel.handlePageFaultLock.acquire();
//        System.out.println("before");
//        for(int i=0;i<pageTable.length;i++){
//            System.out.println("index"+i+"vpn is "+pageTable[i].vpn+" ppn is"+pageTable[i].ppn+" "+pageTable[i].valid+" dirty is "+pageTable[i].dirty);
//        }
//        System.out.println("\n\n");
        System.out.println("In VM Process WrongVaddr is"+wrongVaddr);
        int pageFaultNum=Processor.pageFromAddress(wrongVaddr);
        Lib.assertTrue(pageFaultNum<pageTable.length);
        System.out.println("pagefault num is "+pageFaultNum);
        int spn=pageTable[pageFaultNum].vpn;
        boolean dirty=pageTable[pageFaultNum].dirty;
        boolean readOnly=search(pageFaultNum);
        //Here we only get 1 page a time
        VMKernel.lockForPin.acquire();
        TranslationEntry[] entry=((VMKernel) Kernel.kernel).getPages(1,dirty,pageFaultNum,this);
        VMKernel.lockForPin.release();
        System.out.println(dirty);
        pageTable[pageFaultNum]=new TranslationEntry(entry[0]);
        pageTable[pageFaultNum].dirty=dirty;
        pageTable[pageFaultNum].vpn=spn;
        //!!! cpu will change pageTable, not the page we get(entry[0]), so we need such a hashmap
        VMKernel.TEtable.put(entry[0],pageTable[pageFaultNum]);
        //pageTable[pageFaultNum].vpn=pageFaultNum;
        System.out.println("ppn is "+pageTable[pageFaultNum].ppn);
//        System.out.println(pageTable[pageFaultNum].valid);
//        System.out.println("Coff Section number is"+coff.getNumSections());
        if(pageTable[pageFaultNum].dirty){
            System.out.println("Enter swap");
            swapInPage(pageFaultNum,pageTable[pageFaultNum].ppn);
            pageTable[pageFaultNum].valid = true;
            pageTable[pageFaultNum].readOnly = false;
        }else {
            for (int s = 0; s < coff.getNumSections(); s++) {
                CoffSection section = coff.getSection(s);
                Lib.debug(dbgProcess, "\tinitializing " + section.getName()
                        + " section (" + section.getLength() + " pages)");

                for (int i = 0; i < section.getLength(); i++) {
                    int vpn = section.getFirstVPN() + i;
                    //System.out.println("Coff vpn is"+vpn);
                    if (vpn == pageFaultNum) {
                        System.out.println("Coff vpn is"+vpn);
                        section.loadPage(i, pageTable[vpn].ppn);
                        System.out.println("Finish section load");
                        pageTable[vpn].valid = true;
                        pageTable[vpn].readOnly=section.isReadOnly();
                        VMKernel.handlePageFaultLock.release();
                        return;
                    }

                }
            }
            System.out.println("Stack data init");
            byte[] memory = Machine.processor().getMemory();
            int paddr = pageTable[pageFaultNum].ppn * pageSize;
            Arrays.fill(memory, paddr, paddr + pageSize, (byte) 0);
            pageTable[pageFaultNum].valid = true;
            pageTable[pageFaultNum].readOnly = false;
        }
        VMKernel.handlePageFaultLock.release();
//        System.out.println("after");
//        for(int i=0;i<pageTable.length;i++){
//            System.out.println("index"+i+"vpn is "+pageTable[i].vpn+" ppn is"+pageTable[i].ppn+" "+pageTable[i].valid+" dirty is "+pageTable[i].dirty);
//        }
//        System.out.println("\n\n");
//        VMKernel.locktemp.release();
    }

    public boolean search(int pageFaultNum){
        boolean readOnly=false;
        for (int s = 0; s < coff.getNumSections(); s++) {
            CoffSection section = coff.getSection(s);
            Lib.debug(dbgProcess, "\tinitializing " + section.getName()
                    + " section (" + section.getLength() + " pages)");

            for (int i = 0; i < section.getLength(); i++) {
                int vpn = section.getFirstVPN() + i;
                //System.out.println("Coff vpn is"+vpn);
                if (vpn == pageFaultNum) {
                    readOnly=section.isReadOnly();
                    return readOnly;
                }

            }
        }
        return readOnly;
    }



    public void addPin(int ppn){
        VMKernel.lockForPin.acquire();
        VMKernel.pinnedTable.add(ppn);
        VMKernel.lockForPin.release();
    }

    public void releasePin(int ppn){
        VMKernel.lockForPin.acquire();
        VMKernel.pinnedTable.remove(ppn);
        VMKernel.pinned.wake();
        VMKernel.lockForPin.release();
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
            PTE.used = true;
            addPin(PTE.ppn);
            int physicalAddress = PTE.ppn * 1024 + addressOffset;
            int currentTransferred = Math.min(data.length - offset, Math.min(length, 1024 - addressOffset));
            System.arraycopy(memory, physicalAddress, data, offset, currentTransferred);

            vaddr += currentTransferred;
            offset += currentTransferred;
            length -= currentTransferred;
            amount += currentTransferred;
            releasePin(PTE.ppn);
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
        wVM.acquire();
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
            PTE.used = true;
            System.out.println("before pin in VM");
            addPin(PTE.ppn);
            System.out.println("after pin");

            int physicalAddress = PTE.ppn * 1024 + addressOffset;
            int currentTransferred = Math.min(data.length - offset, Math.min(length, 1024 - addressOffset));
            System.arraycopy(data, offset, memory, physicalAddress, currentTransferred);

            vaddr += currentTransferred;
            offset += currentTransferred;
            length -= currentTransferred;
            amount += currentTransferred;
            releasePin(PTE.ppn);

        }
        System.out.println("finish write virtual memory");
        wVM.release();
        return amount;
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
            case Processor.exceptionPageFault:{
                handlePageFaultException(processor.readRegister(Processor.regBadVAddr));
                break;
            }
            default:
                super.handleException(cause);
                break;
        }
    }

    private static final int pageSize = Processor.pageSize;

    private static final char dbgProcess = 'a';

    private static final char dbgVM = 'v';

    private static Lock wVM=new Lock();


}