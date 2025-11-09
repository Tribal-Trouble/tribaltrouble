package com.oddlabs.net;

import java.io.Serializable;
  
final class BlockingTask implements Task, Serializable {
	 final int id;
	 boolean cancelled;
	 TaskThread.TaskResult result; // Set by task thread

	 BlockingTask(int id) {
		 this.id = id;
	 }

	 @Override
	 public void cancel() {
		 cancelled = true;
	 }
 }
