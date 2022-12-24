package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

import java.util.*;

/**
 * A kernel that can support multiple demand-paging user processes.
 */
public class VMKernel extends UserKernel {
	/**
	 * Allocate a new VM kernel.
	 */
	public VMKernel() {
		super();
	}

	/**
	 * Initialize this kernel.
	 */
	public void initialize(String[] args) {
		super.initialize(args);
		StubFileSystem stubFileSystem= (StubFileSystem) ThreadedKernel.fileSystem;
		swapFile=stubFileSystem.open("swap",true);
		for(int i=0;i<256;i++){
			freeSpn.add(i);
		}
		numberOfSpn=256;
		pagesAvailable=new Lock();
		lockForPin=new Lock();
		pinned=new Condition(lockForPin);
		TEtable=new Hashtable<>();
		handlePageFaultLock=new Lock();
		lock3=new Lock();
		pinnedTable=new HashSet<>();
		invertPT=new Hashtable<>();
		usedPages=new LinkedList<>();
		int n = Machine.processor().getNumPhysPages();
		TranslationEntry tmp = null;
		pages=new LinkedList<>();
		for (int i = 0; i < n; i++)
			//tmp = new TranslationEntry(0, i, false, false, false, false);
			pages.add(new TranslationEntry(i, i, false, false, false, false));

	}

	/**
	 * Test this kernel.
	 */
	public void selfTest() {
		super.selfTest();
	}

	/**
	 * Start running user programs.
	 */
	public void run() {
		super.run();
	}

	/**
	 * Terminate this kernel. Never returns.
	 */
	public void terminate() {
		super.terminate();
	}

	public TranslationEntry colockWiseSelectPage(){
		System.out.println("pin is full");
		System.out.println(usedPages.size());
		System.out.println("pin is full");
		System.out.println(VMKernel.pinnedTable.size());
		Lib.assertTrue(usedPages.size()==Machine.processor().getNumPhysPages());
		while(VMKernel.pinnedTable.size()==Machine.processor().getNumPhysPages()){
			System.out.println("pin is full");
			VMKernel.pinned.sleep();
		}
		while(TEtable.get(usedPages.get(pointer % Machine.processor().getNumPhysPages())).used){
			if(VMKernel.pinnedTable.contains(usedPages.get(pointer % Machine.processor().getNumPhysPages()).ppn)){
				pointer+=1;
				continue;
			}
			TEtable.get(usedPages.get(pointer % Machine.processor().getNumPhysPages())).used=false;
			pointer+=1;

		}
		System.out.println("We here");
		pointer=pointer%Machine.processor().getNumPhysPages();
		return usedPages.get(pointer % Machine.processor().getNumPhysPages());

	}



	public void swapOutPage(TranslationEntry page){
		if(TEtable.get(page).readOnly){
			return;
		}
		if(!TEtable.get(page).dirty){
			return;
		}
		System.out.println("Swap out by Zhenyu");
		byte[] memory = Machine.processor().getMemory();
		byte[] buffer=new byte[Processor.pageSize];
		int spn=-1;
		if(freeSpn.isEmpty()){
			numberOfSpn+=1;
			spn=numberOfSpn-1;
		}
		else{
			spn=freeSpn.remove();
		}
		System.out.println("spn is"+spn);
		TEtable.get(page).vpn=spn;
		TEtable.get(page).valid=false;
		System.arraycopy(memory, page.ppn*Processor.pageSize, buffer, 0, Processor.pageSize);
		swapFile.write(spn*Processor.pageSize,buffer,0,Processor.pageSize);

	}

	public void invalidatePage(TranslationEntry page){

		ProcessAndVpn processandvpn=invertPT.get(page.ppn);
		processandvpn.userProcess.makeInvalid(page.ppn,processandvpn.vpn);
	}

	public TranslationEntry[] getPages(int num,boolean dirty,int vpn,VMProcess vmprocess){
		Lib.assertTrue(num==1);
		System.out.println("Zhenyu getPages." + pages.size());
		TranslationEntry[] res = null;
//		if(pages.isEmpty() || num > pages.size()) return res;
		//lock.acquire();
		if(pages.isEmpty()){
			System.out.println("page is empty");
			TranslationEntry selectedPage=colockWiseSelectPage();
			if(selectedPage==null){
				return null;
			}
			swapOutPage(selectedPage);
			invalidatePage(selectedPage);
			System.out.println("usedPages Size before"+usedPages.size());
			usedPages.remove(selectedPage);
			VMKernel.TEtable.remove(selectedPage);
			System.out.println("usedPages Size now"+usedPages.size());
			pages.add(selectedPage);
		}
		res = new TranslationEntry[num];

		System.out.println("now pages size"+pages.size());
		Iterator<TranslationEntry> iterator = pages.iterator();
		int times = num; // one time for two threads
		int count = 0;
		while(iterator.hasNext() && (times--)>0 ){
			res[count] = iterator.next();
			res[count].used=true;
			res[count].valid = true;
			res[count].dirty=dirty;
			usedPages.add(pointer,res[count]);
			System.out.println("after add used pages num"+usedPages.size());
			ProcessAndVpn processAndVpn=new ProcessAndVpn(vmprocess,vpn);
			invertPT.put(res[count].ppn,processAndVpn);
			pointer=(pointer+1)%Machine.processor().getNumPhysPages();
			iterator.remove();
			count++;
		}
		System.out.println("pages num"+pages.size());
		System.out.println("used pages num"+usedPages.size());
		//lock.release();
		System.out.println("Xingfei xxxx.");
		return res;
	}


	private static class ProcessAndVpn{
		public VMProcess userProcess;
		public int vpn;
		public ProcessAndVpn(VMProcess userProcess, int vpn){
			this.userProcess=userProcess;
			this.vpn=vpn;
		}
	}

	// dummy variables to make javac smarter
	private static VMProcess dummy1 = null;

	public LinkedList<TranslationEntry> pages = new LinkedList <TranslationEntry>();

	public LinkedList<TranslationEntry> usedPages = new LinkedList <TranslationEntry>();

	private static final char dbgVM = 'v';

	public static int pointer=0;

	public static OpenFile swapFile;

	public static LinkedList<Integer> freeSpn = new LinkedList<>();

	public static int numberOfSpn;

	public static Lock pagesAvailable;

	public static Lock lockForPin;

	public static Condition pinned;

	public static Hashtable<TranslationEntry,TranslationEntry> TEtable;

	public static Lock handlePageFaultLock;

	public static Lock lock3;

	public static HashSet<Integer> pinnedTable;

	private static Hashtable<Integer,ProcessAndVpn> invertPT;

}