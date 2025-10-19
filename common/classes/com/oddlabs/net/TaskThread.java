package com.oddlabs.net;

import com.oddlabs.event.Deterministic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TaskThread {
	private final Map<Integer,Callable<?>> id_to_callable = new HashMap<>();
	private final List<BlockingTask> tasks = new ArrayList<>();
	private final List<BlockingTask> finished_tasks = new ArrayList<>();
	private final Object lock = new Object();
	private final Runnable notification_action;
	private int current_id = 0;
	private Thread thread;
	private volatile boolean finished;

	private final Deterministic deterministic;

	public TaskThread(Deterministic deterministic, Runnable notification_action) {
		this.deterministic = deterministic;
		this.notification_action = notification_action;
	}

	interface TaskResult<T> extends Serializable {
		void deliverResult(TaskExecutorLoopbackInterface<T> callback);
	}

	final static class TaskFailed<T> implements TaskResult<T> {
		private final Exception result;

		TaskFailed(Exception e) {
			this.result = e;
		}

        @Override
		public void deliverResult(TaskExecutorLoopbackInterface<T> callback) {
			callback.taskFailed(result);
		}
	}

	final static class TaskSucceeded<T> implements TaskResult<T> {
		private final T result;

		TaskSucceeded(T result) {
			this.result = result;
		}

                @Override
		public void deliverResult(TaskExecutorLoopbackInterface<T> callback) {
			callback.taskCompleted(result);
		}
	}

	private void processTasks() {
		while (!finished) {
			BlockingTask task;
			Callable callable;
			synchronized (lock) {
				while (tasks.isEmpty()) {
					try {
						lock.wait();
					} catch (InterruptedException e) {
						// ignore
					}
				}
				task = tasks.get(0);
				callable = lookupCallable(task);
			}
			TaskResult<?> result;
			try {
				Object callable_result = callable.call();
				result = new TaskSucceeded<>(callable_result);
			} catch (Exception e) {
				result = new TaskFailed<>(e);
			}
			synchronized (lock) {
				task.result = result;
				tasks.remove(0);
				finished_tasks.add(task);
			}
		}
		if (notification_action != null)
			notification_action.run();
	}

	public Deterministic getDeterministic() {
		return deterministic;
	}

	public Task addTask(Callable<?> callable) {
		BlockingTask task;
		synchronized (lock) {
			int task_id = current_id++;
			id_to_callable.put(task_id, callable);
			task = new BlockingTask(task_id);
			tasks.add(task);
			lock.notify();
		}
		if (!deterministic.isPlayback() && thread == null) {
			this.thread = new Thread(this::processTasks);
			this.thread.setName("Task executor thread");
			this.thread.setDaemon(true);
			this.thread.start();
		}
		return task;
	}

	public void poll() {
		while (true) {
			BlockingTask task;
			Callable<?> callable;
			synchronized (lock) {
				if (!deterministic.log(!finished_tasks.isEmpty())) {
					// Check for cancelled task blocking thread
					if (tasks.size() > 0) {
						BlockingTask current_task = tasks.get(0);
						if (current_task.cancelled && thread != null)
							thread.interrupt();
					}
					return;
				}
				task = deterministic.log(deterministic.isPlayback() ? null : finished_tasks.remove(0));
				callable = lookupCallable(task);
			}
			if (!task.cancelled)
				task.result.deliverResult(callable);
		}
	}

	private Callable lookupCallable(BlockingTask task) {
		return id_to_callable.get(task.id);
	}

	public void close() {
		finished = true;
		if (!deterministic.isPlayback())
			thread.interrupt();
	}
}
