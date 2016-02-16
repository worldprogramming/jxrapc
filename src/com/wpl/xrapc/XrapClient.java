package com.wpl.xrapc;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.zeromq.ZMQ;

/**
 * A general purpose client for the XRAP protocol.
 * 
 * For details, see http://rfc.zeromq.org/spec:40
 * @author tomq
 */
public class XrapClient {
	private ZMQ.Socket sock;
	private int receiveTimeoutSeconds = 30;
	private Map<Integer, XrapReply> responseCache = new ConcurrentHashMap<Integer, XrapReply>();
	private Lock lock = new ReentrantLock(); 

	/**
	 * Creates a new XrapClient object using a newly created ZMQ context.
	 * @param endpoint The endpoint to connect to. This should be of the form
	 * tcp://hostname/port (see http://api.zeromq.org/4-0:zmq-tcp)
	 */
	public XrapClient(String endpoint) {
		this(openAndConnect(ZMQ.context(1), endpoint));
	}
	
	/**
	 * Creates a new XrapClient object.
	 * @param zmqContext A ZMQ.Context in which to create the ZMQ socket.
	 * @param endpoint The endpoint to connect to. This should be of the form
	 * tcp://hostname/port (see http://api.zeromq.org/4-0:zmq-tcp)
	 */
	public XrapClient(ZMQ.Context zmqContext, String endpoint) {
		this(openAndConnect(zmqContext, endpoint));
	}

	/**
	 * Creates a new XrapClient using an existing ZMQ socket.
	 * @param sock An existing zmq socket to use.
	 */
	public XrapClient(ZMQ.Socket sock) {
		this.sock = sock;
	}
	
	/**
	 * Sets the timeout for the request, in seconds.
	 * This is 30 by default.
	 * @param seconds The new timeout, in seconds
	 */
	public void setTimeout(int seconds) {
		this.receiveTimeoutSeconds = seconds;
	}
	
	/**
	 * Sends the given request, and blocks waiting for the reply.
	 * @param request An XrapRequest object defining the request to make.
	 * @return An XrapReply representing the reply sent from the server.
	 * @throws XrapException if there is an issue with the XRAP protocol
	 * such as a communication error. Note that the returned XrapReply
	 * will describe errors returned by the server, these are not thrown
	 * as exceptions. 
	 */
	public XrapReply send(XrapRequest request) throws XrapException, InterruptedException {
		sendOnly(request);
		XrapReply response = getResponse(request, receiveTimeoutSeconds, TimeUnit.SECONDS);
		if (response==null) throw new XrapException("Timeout");
		return response;
	}
	
	/**
	 * Makes an asynchronous request.
	 * @param request
	 * @return A {@link java.util.concurrent.Future} object through which the result can be acquired.
	 * If an error occurs receiving the reply, then an {@link java.util.concurrent.ExecutionException} can be thrown
	 * wrapping the underlying {@link XrapException}.
	 * @throws XrapException
	 */
	public Future<XrapReply> sendAsync(XrapRequest request) throws XrapException {
		sendOnly(request);
		return new FutureReply(request);
	}
	
	private void sendOnly(XrapRequest request) throws XrapException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		try {
			request.buildRequest(dos);
		}
		catch (IOException ex) {
			// shouldn't occur when writing to a ByteArrayOutputStream?
		}

		try {
			lock.lock();
			sock.send(new byte[0], ZMQ.SNDMORE);
			sock.send(baos.toByteArray(), 0);
		}
		finally {
			lock.unlock();
		}
	}
	
	private static ZMQ.Socket openAndConnect(ZMQ.Context zmqContext, String endpoint) {
		ZMQ.Socket sock = zmqContext.socket(ZMQ.DEALER);
		sock.connect(endpoint);
		return sock;
	}
	
	private XrapReply getResponse(XrapRequest request) throws XrapException, InterruptedException {
		return getResponse(request, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
	}
	
	
	private XrapReply getResponse(XrapRequest request, long timeout, TimeUnit unit) throws XrapException, InterruptedException {
		XrapReply reply;

		// There are two timeouts. We have to ensure that we return in a time
		// consistent with the timeout passed as argument. We first have to acquire the
		// lock. Another thread may have the lock, and may be waiting on a longer
		// timeout, waiting on the socket. 
		
		long timeoutms = unit.toMillis(timeout);
		while (timeoutms>0) {
			long loopStart = new java.util.Date().getTime();

			// First see whether the response has already been received, either
			// by us previously, or by another thread that might also be waiting.
			if ((reply = responseCache.remove(request.getRequestId()))!=null) {
				return reply;
			}
			
			byte[] responseBytes;
			if (!lock.tryLock(timeoutms, TimeUnit.MILLISECONDS)) return null;
			try {
				sock.setReceiveTimeOut((int)Math.min(timeoutms, Integer.MAX_VALUE));
				responseBytes = sock.recv();
				timeoutms -= new java.util.Date().getTime() - loopStart;
				if (responseBytes==null) {
					// Timed out, or error?
					// Not sure how we tell the difference.
					continue;
				}
				
				// Depending on whether a REQ or DEALER is used, we might get an
				// empty delimiter frame.
				if (responseBytes.length==0)
					responseBytes = sock.recv();
			}
			finally {
				lock.unlock();
			}
			
			reply = request.parseResponse(responseBytes);
			if (reply.requestId == request.getRequestId())
				return reply;
			responseCache.put(request.getRequestId(), reply);
		}
		return null;
	}
	
	private class FutureReply implements Future<XrapReply> {
		private XrapRequest request;
		private XrapReply response;
		private XrapException ex;
		
		public FutureReply(XrapRequest request) {
			this.request = request;
		}
		
		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			// Cancellation not supported.
			return false;
		}

		@Override
		public boolean isCancelled() {
			// Cancellation not supported.
			return false;
		}

		@Override
		public XrapReply get() throws InterruptedException, ExecutionException {
			try {
				if (ex!=null) throw ex;
				if (response==null)
					response = getResponse(request);
				return response;
			}
			catch (XrapException ex) {
				throw new ExecutionException(ex);
			}
		}

		@Override
		public XrapReply get(long timeout, TimeUnit unit)
				throws InterruptedException, ExecutionException, TimeoutException {
			try {
				if (ex!=null) throw ex;
				if (response==null)
					response = getResponse(request, timeout, unit);
				if (response==null)
					throw new TimeoutException();
				return response;
			}
			catch (XrapException ex) {
				throw new ExecutionException(ex);
			}
		}
		
		@Override
		public boolean isDone() {
			try {
				if (response==null)
					response = getResponse(request, 0, TimeUnit.SECONDS);
			}
			catch (XrapException ex) {
				this.ex = ex;
			}
			catch (InterruptedException ex) {
			}
			return response!=null;
		}
	}
}
