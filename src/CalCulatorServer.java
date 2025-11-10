import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CalCulatorServer {
	
	static class ClientHandler implements Runnable { // Runnable 구현
        private Socket socket;
        
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }
        
        @Override
        public void run() {
            BufferedReader in = null;
            BufferedWriter out = null;
            
            try { // 입출력 스트림 구현
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                
                while (true) {
                    String inputMessage = in.readLine(); // 클라이언트에서 읽어온 문장(계산식)
                    
                    if (inputMessage == null || inputMessage.equalsIgnoreCase("bye")) { // 계산식이 비어있거나 bye 일때 
                        System.out.println("클라이언트 연결 종료: " + socket.getInetAddress());
                        break;
                    }
                    
                    try {
                        // 내부 클래스이므로 CalCulatorServer.calc를 바로 호출
                    	// 계산 로직을 calc에게 위임한다. 
                        String res = calc(inputMessage); 
                        out.write(res + "\n");
                        out.flush();
                    } catch (Exception e) {
                        System.err.println("계산 및 응답 오류: " + e.getMessage());
                        // 런타임 오류시 응답 전송
                        String errorRes = buildResponse(500, "SERVER_ERROR", "internal server error");
                        out.write(errorRes + "\n");
                        out.flush();
                        // flush()로 다시 클라이언트에게 보낸다. 
                    }
                }
            } catch (IOException e) {
                System.err.println(e.getMessage());
            } finally {
                try {
                    if (socket != null)
                        socket.close();
                } catch (IOException e) { /* ignore */ }
            }
        }
    }
	public static String buildResponse(int statusCode, String semanticTag, String bodyMessage) {
		return statusCode + "|" + semanticTag + "|" + bodyMessage;
		// 응답 프로토콜 정의
	}
	
	public static String calc(String exp) {
		// 사칙 연산 수행 및 Semantic 오류 검사 
		StringTokenizer st = new StringTokenizer(exp, " ");
		// statusCode, semanticTag, bodyMessage를 |기준으로 쪼갠다. 
		if(st.countTokens() != 3) {
			System.out.println("Incorrect: ");
			System.out.println("Too many/few arguments");
			return buildResponse(400, "INVALID_ARGUMENTS", "too many/few arguments");
		}
		double res;
		String opcode = st.nextToken().toUpperCase(); // 소문자가 들어와도 대문자로 변환
		double op1, op2;
		try {
			op1 = Double.parseDouble(st.nextToken());
			op2 = Double.parseDouble(st.nextToken());
		}catch(NumberFormatException e) {
			return buildResponse(400, "INVALID_OPERAND", "invalid number format");
		}
		switch(opcode) {
		case "ADD" :
			res = op1 + op2;
			System.out.println(op1 + " + " + op2 + " = " + res); // 서버 콘솔에 계산 식 띄우기
			break;
		case "SUB":
			res = op1 - op2;
			System.out.println(op1 + " - " + op2 + " = " + res);
			break;
		case "MUL":
			res = op1 * op2;
			System.out.println(op1 + " * " + op2 + " = " + res);
			break;
		case "DIV":
			if(op2 == 0) {
				System.out.println("Incorrect: ");
				System.out.println("divided by zero");
				return buildResponse(500, "DIVISION_BY_ZERO", "divided by zero");
			}
			res = op1 / op2;
			System.out.println(op1 + " / " + op2 + " = " + res);
			break;
		default:
			return buildResponse(405, "UNSPPORTED_OP", "unsupported operation (" + opcode + ")");
		
		}
				
		return buildResponse(200, "ANSWER", Double.toString(res));
	}

	public static void main(String[] args) {
		// ThreadPool 생성: 다중 클라이언트 동시 처리를 우한 스레드 풀
		// newFixesThePool(10)로 10개 생성
		int port = 9999;
		ExecutorService executor = Executors.newFixedThreadPool(10);
		ServerSocket listener = null;
		if(args.length > 0) {
			try {
				port = Integer.parseInt(args[0]);
			}catch(NumberFormatException e) {
				System.out.println(e.getMessage());
			}
		}
		try {
			listener = new ServerSocket(port);
			System.out.println("연결을 기다리는 중.....");
			
			while(true) {
				Socket socket = listener.accept(); // 클라이언트 연결 대기 및 수락
				System.out.println("연결되었습니다. 클라이언트 IP: " + socket.getInetAddress());
				
				// ClientHandler 생성 후 ThreadPool에 작업 할당 
				ClientHandler clientHandler = new ClientHandler(socket);
				executor.execute(clientHandler);
			}
		}catch(IOException e) {
			System.out.println(e.getMessage());
		}finally {
			try {
				if(listener != null)
					listener.close();
			}catch(IOException e) {
				System.out.println("채팅 중 오류 발생");
			}
			executor.shutdown();
		}
		
	}

}
