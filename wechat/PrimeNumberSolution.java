/**
 * 
 */
package com.bz.cy.app.wechat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 取质数，质数只能被自己和1 整除
 * @author 作者：yuanping
 * @version 创建时间：2022-2-22下午03:52:09
 * @description:
 */
public class PrimeNumberSolution {

	/**
	 * 	Executors.newSingleThreadExecutor();//单一的线程池
		Executors.newFixThreadPool(int n);//固定大小的线程池
		Executors.newCacheThreadPool();//不固定大小的线程池。
		executors底层其实也是调用的new ThreadPoolExecutor()的方式创建的,是对不同线程池的封装
		线程的执行有两种方式,一种是submit(runnable v)的形式,一种是execute(runnable b) 的形式,不同的是submit可以返回一个future的实现类,相同的一点是submit底层其实也是调用的execute
	 	
	 	java线程池有shutdown方法。线程池的shutdown表示线程池执行完所有任务后将关闭线程池。线程池需要调用shutdown方法，不然主程序会一直处于执行中。这是告诉线程池任务执行完毕后将关闭线程池，shutdown方法不是立刻停止当前的线程池任务。而是等待线程池任务执行完毕后，将关闭线程池,不在接收任务添加了
	 * @param args
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws InterruptedException {
		Integer count = 100000;
		long start = System.currentTimeMillis();
		PrimeNumberContext singlePrime = new PrimeNumberContext(count);
		ExecutorService single = Executors.newSingleThreadExecutor();
		for (int i = 0; i < count; i++) {
			single.submit(new ComputePrime(singlePrime));
		}
		singlePrime.countDownLatch.await();
		single.shutdown();
		System.out.println("1个线程用时："+ (System.currentTimeMillis() - start) +"("+ singlePrime.getNum() + " / "+ singlePrime.getPrimeCount() +")");

		start = System.currentTimeMillis();
		/**
		 * Runtime.getRuntime().availableProcessors()
		 * 这行代码获取的是CPU的处理器数量,也就是计算资源。
		 * 询问jvm，jvm去问操作系统，操作系统去问硬件
		 * 
		 * cpu密集型计算推荐设置线程池核心线程数为N，也就是和cpu的线程数相同，可以尽可能低避免线程间上下文切换。
		 * io密集型计算推荐设置线程池核心线程数为2N，但是这个数一般根据业务压测出来的，如果不涉及业务就使用推荐。
		 */
		ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		PrimeNumberContext multiPrime = new PrimeNumberContext(count);
		for (int i = 0; i < count; i++) {
			executorService.submit(new ComputePrime(multiPrime));
		}
		//等待60s后,count值还没变为0的话就会继续执行
		multiPrime.countDownLatch.await(60, TimeUnit.SECONDS);
		executorService.shutdown();
		System.out.println(Runtime.getRuntime().availableProcessors() + "个线程用时：" + (System.currentTimeMillis() - start) + "(" + multiPrime.getNum() + " / " + multiPrime.getPrimeCount() + ")");
	}
}

class PrimeNumberContext {
	AtomicInteger num;
	AtomicInteger primeCount;
	CountDownLatch countDownLatch;

	public PrimeNumberContext(int limit) {
		num = new AtomicInteger(1);
		primeCount = new AtomicInteger(0);
		countDownLatch = new CountDownLatch(limit);
	}
	//AtomicInteger.getAndIncrement()，返回的是当前值
	public int getNum() {
		return num.getAndIncrement();
	}
	//AtomicInteger.incrementAndGet()，返回的是加1后的值
	public void addPrime() {
		primeCount.incrementAndGet();
	}

	public AtomicInteger getPrimeCount() {
		return primeCount;
	}
	/**
	 * countDownLatch这个类使一个线程等待其他线程各自执行完毕后再执行。
	 * 是通过一个计数器来实现的，计数器的初始值是线程的数量。每当一个线程执行完毕后，计数器的值就-1，当计数器的值为0时，表示所有线程都执行完毕，然后在闭锁上等待的线程就可以恢复工作了。
	 * 	//调用await()方法的线程会被挂起，它会等待直到count值为0才继续执行
			public void await() throws InterruptedException { };   
		//和await()类似，只不过等待一定的时间后count值还没变为0的话就会继续执行
			public boolean await(long timeout, TimeUnit unit) throws InterruptedException { };  
		//将count值减1
			public void countDown() { };  
	 */
	public void decreaseLatch() {
		countDownLatch.countDown();
	}
}

class ComputePrime implements Runnable {
	PrimeNumberContext primeNumber;

	public ComputePrime(PrimeNumberContext primeNumber) {
		this.primeNumber = primeNumber;
	}

	/**
	 * 每次完成一个数字判定，就计数-1
	 */
	@Override
	public void run() {
		if (checkNumBinary(primeNumber.getNum())) {
			primeNumber.addPrime();
		}
		primeNumber.decreaseLatch();
	}

	/**
	 * 这个二分的效率太高，导致单线程计算都很快。
	 * @param num
	 * @return
	 */
//		System.out.printf("e 的值为 %.4f%n", Math.E);
//		System.out.printf("sqrt(%.3f) 为 %.3f%n", 11.635, Math.sqrt(11.635));
//		e 的值为 2.7183
//		sqrt(11.635) 为 3.411
	public boolean checkNumBinary(int num) {
		//Math.sqrt(),方法用于返回参数的算术平方根
		int limit = (int) Math.sqrt(num);
		for (int i = 2; i <= limit; i++) {
			if (num % i == 0) return false;
		}
		return true;
	}

	/**
	 * 这个CPU消耗大一点，多CPU能看出差距
	 * @param num
	 * @return
	 */
	public boolean checkNum(int num) {
		for (int i = num - 1; i >= 1; i--) {
			int gcd = gcd(num, i);
			if (gcd != 1) {
				return false;
			}
		}
		return true;
	}

	public int gcd(int a, int b) {
		return b == 0 ? a : gcd(b, a % b);
	}
}