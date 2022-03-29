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
 * ȡ����������ֻ�ܱ��Լ���1 ����
 * @author ���ߣ�yuanping
 * @version ����ʱ�䣺2022-2-22����03:52:09
 * @description:
 */
public class PrimeNumberSolution {

	/**
	 * 	Executors.newSingleThreadExecutor();//��һ���̳߳�
		Executors.newFixThreadPool(int n);//�̶���С���̳߳�
		Executors.newCacheThreadPool();//���̶���С���̳߳ء�
		executors�ײ���ʵҲ�ǵ��õ�new ThreadPoolExecutor()�ķ�ʽ������,�ǶԲ�ͬ�̳߳صķ�װ
		�̵߳�ִ�������ַ�ʽ,һ����submit(runnable v)����ʽ,һ����execute(runnable b) ����ʽ,��ͬ����submit���Է���һ��future��ʵ����,��ͬ��һ����submit�ײ���ʵҲ�ǵ��õ�execute
	 	
	 	java�̳߳���shutdown�������̳߳ص�shutdown��ʾ�̳߳�ִ������������󽫹ر��̳߳ء��̳߳���Ҫ����shutdown��������Ȼ�������һֱ����ִ���С����Ǹ����̳߳�����ִ����Ϻ󽫹ر��̳߳أ�shutdown������������ֹͣ��ǰ���̳߳����񡣶��ǵȴ��̳߳�����ִ����Ϻ󣬽��ر��̳߳�,���ڽ������������
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
		System.out.println("1���߳���ʱ��"+ (System.currentTimeMillis() - start) +"("+ singlePrime.getNum() + " / "+ singlePrime.getPrimeCount() +")");

		start = System.currentTimeMillis();
		/**
		 * Runtime.getRuntime().availableProcessors()
		 * ���д����ȡ����CPU�Ĵ���������,Ҳ���Ǽ�����Դ��
		 * ѯ��jvm��jvmȥ�ʲ���ϵͳ������ϵͳȥ��Ӳ��
		 * 
		 * cpu�ܼ��ͼ����Ƽ������̳߳غ����߳���ΪN��Ҳ���Ǻ�cpu���߳�����ͬ�����Ծ����ܵͱ����̼߳��������л���
		 * io�ܼ��ͼ����Ƽ������̳߳غ����߳���Ϊ2N�����������һ�����ҵ��ѹ������ģ�������漰ҵ���ʹ���Ƽ���
		 */
		ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		PrimeNumberContext multiPrime = new PrimeNumberContext(count);
		for (int i = 0; i < count; i++) {
			executorService.submit(new ComputePrime(multiPrime));
		}
		//�ȴ�60s��,countֵ��û��Ϊ0�Ļ��ͻ����ִ��
		multiPrime.countDownLatch.await(60, TimeUnit.SECONDS);
		executorService.shutdown();
		System.out.println(Runtime.getRuntime().availableProcessors() + "���߳���ʱ��" + (System.currentTimeMillis() - start) + "(" + multiPrime.getNum() + " / " + multiPrime.getPrimeCount() + ")");
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
	//AtomicInteger.getAndIncrement()�����ص��ǵ�ǰֵ
	public int getNum() {
		return num.getAndIncrement();
	}
	//AtomicInteger.incrementAndGet()�����ص��Ǽ�1���ֵ
	public void addPrime() {
		primeCount.incrementAndGet();
	}

	public AtomicInteger getPrimeCount() {
		return primeCount;
	}
	/**
	 * countDownLatch�����ʹһ���̵߳ȴ������̸߳���ִ����Ϻ���ִ�С�
	 * ��ͨ��һ����������ʵ�ֵģ��������ĳ�ʼֵ���̵߳�������ÿ��һ���߳�ִ����Ϻ󣬼�������ֵ��-1������������ֵΪ0ʱ����ʾ�����̶߳�ִ����ϣ�Ȼ���ڱ����ϵȴ����߳̾Ϳ��Իָ������ˡ�
	 * 	//����await()�������̻߳ᱻ��������ȴ�ֱ��countֵΪ0�ż���ִ��
			public void await() throws InterruptedException { };   
		//��await()���ƣ�ֻ�����ȴ�һ����ʱ���countֵ��û��Ϊ0�Ļ��ͻ����ִ��
			public boolean await(long timeout, TimeUnit unit) throws InterruptedException { };  
		//��countֵ��1
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
	 * ÿ�����һ�������ж����ͼ���-1
	 */
	@Override
	public void run() {
		if (checkNumBinary(primeNumber.getNum())) {
			primeNumber.addPrime();
		}
		primeNumber.decreaseLatch();
	}

	/**
	 * ������ֵ�Ч��̫�ߣ����µ��̼߳��㶼�ܿ졣
	 * @param num
	 * @return
	 */
//		System.out.printf("e ��ֵΪ %.4f%n", Math.E);
//		System.out.printf("sqrt(%.3f) Ϊ %.3f%n", 11.635, Math.sqrt(11.635));
//		e ��ֵΪ 2.7183
//		sqrt(11.635) Ϊ 3.411
	public boolean checkNumBinary(int num) {
		//Math.sqrt(),�������ڷ��ز���������ƽ����
		int limit = (int) Math.sqrt(num);
		for (int i = 2; i <= limit; i++) {
			if (num % i == 0) return false;
		}
		return true;
	}

	/**
	 * ���CPU���Ĵ�һ�㣬��CPU�ܿ������
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