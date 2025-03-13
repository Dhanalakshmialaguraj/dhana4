import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

class BankAccount {
    private final int accountNumber;
    private double balance;
    private final ReentrantLock lock = new ReentrantLock();

    public BankAccount(int accountNumber, double initialBalance) {
        this.accountNumber = accountNumber;
        this.balance = initialBalance;
    }

    public boolean transferTo(BankAccount target, double amount) {
        if (this == target || amount <= 0) return false;

        // Avoid deadlock by ordering locks based on account number
        BankAccount firstLock = this.accountNumber < target.accountNumber ? this : target;
        BankAccount secondLock = this.accountNumber < target.accountNumber ? target : this;

        firstLock.lock.lock();
        try {
            secondLock.lock.lock();
            try {
                if (balance >= amount) {
                    this.balance -= amount;
                    target.balance += amount;
                    System.out.printf("Transferred $%.2f from Acc#%d to Acc#%d%n", amount, this.accountNumber, target.accountNumber);
                    return true;
                } else {
                    System.out.printf("Failed transfer: Insufficient funds in Acc#%d%n", this.accountNumber);
                    return false;
                }
            } finally {
                secondLock.lock.unlock();
            }
        } finally {
            firstLock.lock.unlock();
        }
    }

    public void printBalance() {
        System.out.printf("Acc#%d Balance: $%.2f%n", accountNumber, balance);
    }
}

class TransferThread extends Thread {
    private final List<BankAccount> accounts;
    private final Random random = new Random();
    private final int iterations;

    public TransferThread(List<BankAccount> accounts, int iterations) {
        this.accounts = accounts;
        this.iterations = iterations;
    }

    @Override
    public void run() {
        for (int i = 0; i < iterations; i++) {
            int fromIndex = random.nextInt(accounts.size());
            int toIndex = random.nextInt(accounts.size());
            double amount = 10 + (1000 * random.nextDouble());
            accounts.get(fromIndex).transferTo(accounts.get(toIndex), amount);

            try {
                Thread.sleep(random.nextInt(100)); // simulate delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

public class BankSimulation {
    public static void main(String[] args) {
        List<BankAccount> accounts = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            accounts.add(new BankAccount(i, 10000.0));
        }

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            threads.add(new TransferThread(accounts, 10));
        }

        threads.forEach(Thread::start);
        threads.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        System.out.println("\nFinal Balances:");
        accounts.forEach(BankAccount::printBalance);
    }
}
