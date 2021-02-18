package utils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * This class emulates a "compare and swap"-style spin-lock with
 * non-recursive semantics using Java VarHandle.
 */
public class NonReentrantSpinLock
       implements Lock {
    /**
     * The VarHandle used to access the 'value' field below.
     */
    private static final VarHandle VALUE;

    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            // Initialize the VarHandle via reflection.
            VALUE = l.findVarHandle(NonReentrantSpinLock.class, "value", int.class);
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    /**
     * The value used to implement the spin-lock.
     */
    private volatile int value;

    /**
     * Acquire the lock only if it is free at the time of invocation.
     * Acquire the lock if it is available and returns immediately
     * with the value true.  If the lock is not available then this
     * method will return immediately with the value false.
     */
    @Override
    public boolean tryLock() {
        // Try to set owner's value to true, which succeeds iff its
        // current value is false.
        return VALUE.compareAndSet(this, 0, 1);
    }

    /**
     * Acquire the lock non-interruptibly. If the lock is not
     * available then the current thread spins until the lock has been
     * acquired.
     */
    @Override
    public void lock() {
        // Loop trying to set the owner's value to true, which
        // succeeds iff its current value is false.  Each iteration
        // should also check if a shutdown has been requested and if
        // so throw a cancellation exception.

        for (; ;) {
            // Only try to get the lock if its null, which improves
            // cache performance.  See the stackoverflow article at
            // http://15418.courses.cs.cmu.edu/spring2013/article/31
            // and stackoverflow.com/a/4939383/13411862 for details.
            if (value == 0 && tryLock()) 
                // Break out of the loop if we got the lock.
                break;
        }
    }

    /**
     * Acquire the lock interruptibly. If the lock is not available
     * then the current thread spins until the lock has been acquired.
     */
    @Override
    public void lockInterruptibly() throws InterruptedException {
        for (; ;) {
            // Only try to get the lock if its null, which improves
            // cache performance.
            if (value == 0 && tryLock())
                // Break out of the loop if we got the lock.
                break;
            else if (Thread.interrupted())
                // Break out of the loop if we were interrupted.
                break;
        }
    }

    /**
     * Release the lock.  Throws IllegalMonitorStateException if the
     * calling thread doesn't own the lock.
     */
    @Override
    public void unlock() {
        // Atomically release the lock that's currently held by the
        // owner. If the lock is not held by the owner, then throw an
        // IllegalMonitorStateException.
        if ((int)VALUE.getAndSet(this, 0) != 1)
            throw new IllegalMonitorStateException("Unlock called when not locked");
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        throw new NotImplementedException();
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public Condition newCondition() {
        throw new NotImplementedException();
    }
}
