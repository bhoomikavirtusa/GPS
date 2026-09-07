package com.wiley.permissions.security.web;

/**
 * Stores a ThreadLocal userId for adding audit data to transactions (or anything in the same thread).
 *
 * @author smarkoff
 */
public final class ThreadLocalUser
{
	private static ThreadLocal<UserPrincipal> threadLocal = new ThreadLocal<UserPrincipal>();

	public static void set(UserPrincipal user) {
		threadLocal.set(user);
	}
	
	public static UserPrincipal get() {
		return threadLocal.get();
	}

	/**
	 * smarkoff: In terms of memory usage it doesn't matter much if this
	 * method is never called because we are only storing a simple Integer
	 * and ThreadLocal variables should be cleaned up by garbage collection
	 * when the Thread dies.
	 *
	 * However, it may be desirable/important to call this method to ensure
	 * that a thread being used to service different users does not carry
	 * over stale data to another user.
	 */
	public static void cleanup() {
		threadLocal.remove();
	}
}
