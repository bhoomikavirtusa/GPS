package com.wiley.permissions.common.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author ttidwell
 */
public class InstanceOnlySet<T extends Object>
implements Set<T>
{
	@SuppressWarnings("unused")
	private final static Log log = LogFactory.getLog(InstanceOnlySet.class);

	private T[] list = null;

	@Override
	public boolean add(T e) {
		boolean output = false;

		if (!contains(e) && e != null) {
			if (list == null) {
				list = (T[]) new Object[] { e };
			}
			else {
				T[] newList = Arrays.copyOf(list, list.length + 1);
				newList[list.length] = e;
				list = newList;
			}
		}

		return output;
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		boolean output = false;

		for (T o : c) {
			if (add(o)) {
				output = true;
			}
		}

		return output;
	}

	@Override
	public void clear() {
		list = null;
	}

	@Override
	public boolean contains(Object o) {
		boolean output = false;

		if (o != null) {
			if (list != null) {
				for (Object obj : list) {
					if (obj == o) {
						output = true;
						break;
					}
				}
			}
		}

		return output;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		boolean output = true;

		Iterator<?> it = c.iterator();

		while (it.hasNext() && output) {
			output = contains(it.next());
		}

		return output;
	}

	@Override
	public boolean isEmpty() {
		return (list == null || list.length == 0);
	}

	@Override
	public Iterator<T> iterator() {
		List<T> tmp = getAll();

		return tmp.iterator();
	}

	private List<T> getAll() {
		List<T> output = new ArrayList<T>();

		if (list != null) {
			output = Arrays.asList(list);
		}

		return output;
	}

	@Override
	public boolean remove(Object o) {
		boolean output = false;

		if (o != null) {
			int index = -1;

			if (list != null) {
				for (int x = 0; x < list.length && index == -1; x++) {
					if (o == list[x]) {
						index = x;
					}
				}

				if (index != -1) {
					output = true;

					T[] newList = Arrays.copyOfRange(list, 0, list.length - 1);

					if (newList.length > 0) {
						System.arraycopy(list, index, newList, index, newList.length);
					}

					list = newList;
				}
			}
		}

		return output;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		boolean output = false;

		for (Object o : c) {
			if (remove(o)) {
				output = true;
			}
		}

		return output;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		boolean output = true;

		List<T> tmpList = new ArrayList<T>();

		for (Object o : c) {
			if (contains(o)) {
				tmpList.add((T) o);
			}
		}

		clear();

		for (T o : tmpList) {
			add(o);
		}

		return output;
	}

	@Override
	public int size() {
		int output = 0;

		if (list != null) {
			output = list.length;
		}

		return output;
	}

	@Override
	public Object[] toArray() {
		return getAll().toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return getAll().toArray(a);
	}
}
