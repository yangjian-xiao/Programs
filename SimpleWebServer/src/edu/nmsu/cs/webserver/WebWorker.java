package edu.nmsu.cs.webserver;

/**
 * Web worker: an object of this class executes in its own new thread to receive and respond to a
 * single HTTP request. After the constructor the object executes on its "run" method, and leaves
 * when it is done.
 *
 * One WebWorker object is only responsible for one client connection. This code uses Java threads
 * to parallelize the handling of clients: each WebWorker runs in its own thread. This means that
 * you can essentially just think about what is happening on one client at a time, ignoring the fact
 * that the entirety of the webserver execution might be handling other clients, too.
 *
 * This WebWorker class (i.e., an object of this class) is where all the client interaction is done.
 * The "run()" method is the beginning -- think of it as the "main()" for a client interaction. It
 * does three things in a row, invoking three methods in this class: it reads the incoming HTTP
 * request; it writes out an HTTP header to begin its response, and then it writes out some HTML
 * content for the response content. HTTP requests and responses are just lines of text (in a very
 * particular format).
 * 
 * @author Jon Cook, Ph.D.
 *
 **/
import java.io.File;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;

public class WebWorker implements Runnable
{

	private Socket socket;
	private File file; 
	private String path;
	private String fileType;

	/**
	 * Constructor: must have a valid open socket
	 **/
	public WebWorker(Socket s)
	{
		socket = s;
	}

	/**
	 * Worker thread starting point. Each worker handles just one HTTP request and then returns, which
	 * destroys the thread. This method assumes that whoever created the worker created it with a
	 * valid open socket object.
	 **/
	public void run()
	{
		System.err.println("Handling connection...");
		try
		{
			InputStream is = socket.getInputStream();
			OutputStream os = socket.getOutputStream();
			readHTTPRequest(is);
			file = new File(path);
			fileType = path.substring(path.lastIndexOf(".")+1);
			if(file.exists() && file.isFile())
			{
				//determine file type and write file header
				if (fileType.equals("png"))
				{
					writeHTTPHeader(os, "image/png");
				}
				else if (fileType.equals("gif")) 
				{
					writeHTTPHeader(os, "image/gif");
				}
				else if (fileType.equals("jpg"))
				{
					writeHTTPHeader(os, "image/jpeg");
				}
				else
				{
					writeHTTPHeader(os, "text/html");
				}

				//output file content
				writeContent(os);
			}
			else
			{
				// output error page
				writeHTTPHeader(os, "text/html");
				os.write("<html><head></head>".getBytes());
				os.write("<body><h1><center>404 Error Page Not Found</center></h1></body></html>".getBytes());
			}
			os.flush();
			socket.close();
		}
		catch (Exception e)
		{
			System.err.println("Output error: " + e);
		}
		System.err.println("Done handling connection.");
		return;
	}

	/**
	 * Read the HTTP request header.
	 **/
	private void readHTTPRequest(InputStream is)
	{
		String line;
		BufferedReader r = new BufferedReader(new InputStreamReader(is));
		while (true)
		{
			try
			{
				while (!r.ready())
					Thread.sleep(1);
				line = r.readLine();
				System.err.println("Request line: (" + line + ")");
				if (line.length() == 0)
					break;	

				if (line.substring(0,3).equals("GET"))
				{
					String[] parts = line.split(" ");
					path = "." + parts[1];
					System.out.println(path);
					if(path.equals("./"))
					{
						System.out.println("server is working!");
						//turn to default html path
						path = "./www/res/default.html";
						System.out.println("request file: ");
					}

				}
						
			}
			catch (Exception e)
			{
				System.err.println("Request error: " + e);
				break;
			}
		}
		return;
	}

	/**
	 * Write the HTTP header lines to the client network connection.
	 * 
	 * @param os
	 *          is the OutputStream object to write to
	 * @param contentType
	 *          is the string MIME content type (e.g. "text/html")
	 **/
	private void writeHTTPHeader(OutputStream os, String contentType) throws Exception
	{
		//Date d = new Date();
		//DateFormat df = DateFormat.getDateTimeInstance();
		//df.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		os.write("HTTP/1.1 200 OK\n".getBytes());
		// if(file.exists() && file.isFile())
		// {
		// 	//os.write("HTTP/1.1 200 OK\n".getBytes());
		// 	os.write("HTTP".getBytes());
		// }
		// else
		// {
		// 	os.write(" ".getBytes());
		// }
		//os.write("Date: ".getBytes());
		//os.write((df.format(d)).getBytes());
		//os.write("\n".getBytes());
		//os.write("Server: Xiao's very own server\n".getBytes());
		// os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
		// os.write("Content-Length: 438\n".getBytes());
		//os.write("Connection: close\n".getBytes());
		os.write("Content-Type: ".getBytes());
		os.write(contentType.getBytes());
		os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
		return;
	}

	/**
	 * Write the data content to the client network connection. This MUST be done after the HTTP
	 * header has been written out.
	 * 
	 * @param os
	 *          is the OutputStream object to write to
	 **/
	private void writeContent(OutputStream os) throws Exception
	{
		if(fileType.equals("html"))
		{
			//output html
			BufferedReader b = new BufferedReader(new FileReader(file));
			String s;
			Date d = new Date();
			DateFormat df = DateFormat.getDateTimeInstance();
			df.setTimeZone(TimeZone.getTimeZone("GMT"));
			while((s=b.readLine()) != null)
			{
				s = s.replaceAll("<cs371date>", df.format(d));
				s = s.replaceAll("<cs371server>", "xiao's server");
				os.write(s.getBytes());
			}
			b.close();
		}
		else
		{
			//output image
			//read inputstream for local image
			FileInputStream fis = new FileInputStream(path);
			int i = fis.available();
			//byte array is used to store bite data of the image
			byte[] buff = new byte[i];
			fis.read(buff);
			fis.close();
			os.write(buff);

		}
		//os.write("<html><head></head><body>\n".getBytes());
		//os.write("<h3>My web server works!</h3>\n".getBytes());
		//os.write("</body></html>\n".getBytes());
	}

} // end class
