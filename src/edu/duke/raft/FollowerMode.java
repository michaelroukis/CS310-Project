package edu.duke.raft;

import java.util.Random;
import java.util.Timer;

public class FollowerMode extends RaftMode {

	private int timeout;
	private Timer timer;

	private boolean hasVoted = false;

	public void go () {
		synchronized (mLock) {
			int term = mConfig.getCurrentTerm();
			//Set up timer
			Random myRandom = new Random();
			timeout = ELECTION_TIMEOUT_MIN + myRandom.nextInt(ELECTION_TIMEOUT_MAX - ELECTION_TIMEOUT_MIN);
			timer = scheduleTimer(timeout, 1);
			System.out.println ("S" + 
					mID + 
					"." + 
					term + 
					": switched to follower mode.");
		}
	}

	// @param candidate’s term
	// @param candidate requesting vote
	// @param index of candidate’s last log entry
	// @param term of candidate’s last log entry
	// @return 0, if server votes for candidate; otherwise, server's
	// current term
	public int requestVote (int candidateTerm, int candidateID, int lastLogIndex, int lastLogTerm) {
		synchronized (mLock) {
			int term = mConfig.getCurrentTerm ();
			if (hasVoted) {
				return term;
			}
			else {
				hasVoted = true;
				// we vote for the candidate. their term is equal-to or higher-than us
				mConfig.setCurrentTerm(candidateTerm, candidateID);
				resetTimer();
				return 0;
			}
		}
	}

	private void resetTimer() {

		timer.cancel();
		timer = scheduleTimer(timeout, 1);

	}

	// @param leader’s term
	// @param current leader
	// @param index of log entry before entries to append
	// @param term of log entry before entries to append
	// @param entries to append (in order of 0 to append.length-1)
	// @param index of highest committed entry
	// @return 0, if server appended entries; otherwise, server's
	// current term
	public int appendEntries (int leaderTerm,
			int leaderID,
			int prevLogIndex,
			int prevLogTerm,
			Entry[] entries,
			int leaderCommit) {
		
		synchronized (mLock) {
			hasVoted = false;
			resetTimer();
			 //0 if added, currentTerm if nothing added
			return performLogRollbackCheck(prevLogIndex, prevLogTerm, entries, leaderCommit);
			
		}
		
	}  
	
	private int performLogRollbackCheck(int prevLogIndex, int prevLogTerm, Entry[] entries, int leaderCommit) {
		int currLogIndex = mLog.getLastIndex();
		// Was just a leader, but superceded by a newer leader
		if (leaderCommit < currLogIndex) {
			removeLastFewEntriesFromLog(prevLogIndex, prevLogTerm, currLogIndex, leaderCommit);
		}
		// Append whatever the Leader tells us to. We're a Follower.
		return appendLeaderEntries(entries, prevLogIndex, prevLogTerm, currLogIndex, leaderCommit);
		
	}

	private void removeLastFewEntriesFromLog(int prevLogIndex, int prevLogTerm, int currLogIndex, int leaderCommit) {
		int quantityOfEntriesToRemove = currLogIndex - leaderCommit;
		for (int i = 0; i < quantityOfEntriesToRemove; i++) {
//			mLog.insert(entries, prevIndex, prevTerm)
		}
	}
	
	/**
	 * @param entries
	 * @param currLogIndex
	 * @return 0 if added entries, currentTerm if none added
	 */
	private int appendLeaderEntries(Entry[] entries, int prevLogIndex, int prevLogTerm, int currLogIndex, int leaderCommit) {
		
		int newLogIndex;
		int currentTerm = mConfig.getCurrentTerm();
		if (entries == null) {
			return currentTerm;
		}
		else {
			newLogIndex = mLog.append(entries);
			// Attempted append, but it was empty. nothing added.
			if (newLogIndex == currLogIndex) {
				return currentTerm;
			}
			else {
				return 0;
			}
			
		}
		
	}

	// @param id of the timer that timed out
	public void handleTimeout (int timerID) {
		synchronized (mLock) {

			// cancel old Timer-thread to avoid accidental, delayed messages
			timer.cancel();
			RaftServerImpl.setMode(new CandidateMode());

		}
	}
}

