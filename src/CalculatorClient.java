import java.io.*;
import java.net.*;
import java.util.*;

class ServerConfig {
	// 서버 접속 정보를 담는 데이터 클래스 
	public final String ip;
	public final int port;
	
	public ServerConfig(String ip, int port) {
		this.ip = ip;
		this.port = port;
	}
}
public class CalculatorClient {
	private static ServerConfig readServerConfig(String filename) {
		String defaultIP = "localhost";
		int defaultPort = 9999;
		
		try(BufferedReader reader = new BufferedReader(new FileReader(filename))){
			String ip = reader.readLine(); // 읽어오기
			String portStr = reader.readLine(); // 읽어오기
			
			if (ip != null && portStr != null) {
                // ... (생략: 유효성 검사 및 파싱)	
                int port = Integer.parseInt(portStr.trim()); // 공백 제거
                System.out.println("Config loaded: " + ip.trim() + " : " + port);
                return new ServerConfig(ip.trim(), port);
            }
        } catch (Exception e) {
            // 파일이 없거나(FileNotFoundException) 파싱 오류 시 기본값 사용
        	e.printStackTrace(); // 에러 파악이 가능하다. 
            System.out.println("Config file error or not found. Using default (" + defaultIP + ":" + defaultPort + ").");
        }
        return new ServerConfig(defaultIP, defaultPort);
    }
	public static void main(String[] args) {
		BufferedReader in = null;
		BufferedReader stin = null;
		BufferedWriter out = null;
		Socket socket = null;
		
		ServerConfig config = readServerConfig("server_info.dat");
		
		try {
			socket = new Socket(config.ip, config.port); // 서버와 연결
			
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			stin = new BufferedReader(new InputStreamReader(System.in));
			out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			
			while(true) {
				System.out.println("계산식 입력 >>");
				String outputMessage = stin.readLine(); // 계산식 입력
				if(outputMessage.equalsIgnoreCase("bye")) {
					out.write(outputMessage + "\n");
					out.flush();
					break;
				}
				
				out.write(outputMessage + "\n");
				out.flush(); // 서버로 보낸다. 
				
				String inputMessage = in.readLine(); // 서버로부터 온 프로토콜
				String[] parts = inputMessage.split("\\|", 3); // 3개로 분리 
				
				String semanticTag = parts[1]; // semanticTag
				String messageBody = parts[2]; // bodyMessage
				
				switch (semanticTag) {
				case "ANSWER":
					// 성공 응답: Answer: 30
					System.out.println("Answer: " + messageBody);
					break;
				case "INVALID_ARGUMENTS":
				case "DIVISION_BY_ZERO":
				case "UNSUPPORTED_OP":
				case "INVALID_OPERAND":
					// 오류 응답: Error message: divided by zero
					System.out.println("Error message: " + messageBody);
					break;
				default:
					System.out.println("Unknown Response Tag: " + inputMessage);
				}
			}
		}catch(IOException e) {
			System.out.println(e.getMessage());
		}finally {
			try {
				if(socket != null)
					socket.close();
			}catch(IOException e) {
				System.out.println("서버와 채팅중 오류 발생");
			}
		}

	}
}
