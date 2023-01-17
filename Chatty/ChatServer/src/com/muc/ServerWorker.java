package com.muc;

import java.io.*;
import java.net.Socket;
import org.apache.commons.lang3.StringUtils;
import java.util.HashSet;
import java.util.List;


public class ServerWorker extends Thread{
    private final Socket socket;
    private final Server server;
    private String login = null;
    private OutputStream outputStream;
    private HashSet<String> topicSet = new HashSet<>();

    public ServerWorker(Server server,Socket clientSocket) {
        this.socket=clientSocket;
        this.server=server;
    }

    @Override
    public void run(){
        try {
            handleClientSocket();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void handleClientSocket() throws IOException, InterruptedException{
        InputStream inputStream = socket.getInputStream();
        this.outputStream = socket.getOutputStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = reader.readLine()) != null){
            String[] tokens = StringUtils.split(line);
            if(tokens != null && tokens.length > 0) {
                String cmd = tokens[0];
                if ("logoff".equals(cmd) || "quit".equalsIgnoreCase(cmd)) {
                    handleLogoff();
                    break;
                }else if("login".equalsIgnoreCase(cmd)) {
                    handleLogin(outputStream, tokens);
                }else if ("msg".equalsIgnoreCase(cmd)){
                    String[] tokensMsg = StringUtils.split(line, null, 3);
                    handleMessage(tokensMsg);
                }else if("join".equalsIgnoreCase(cmd)){
                    handleJoin(tokens);
                }else if("leave".equalsIgnoreCase(cmd)){
                    handleLeave(tokens);
                }else {
                    String msg = "unknown " + cmd + "\n";
                    outputStream.write(msg.getBytes());
                }
            }
        }

        socket.close();
    }

    private void handleLeave(String[] tokens) {
        if(tokens.length>1){
            String topic = tokens[1];
            topicSet.remove(topic);
        }
    }

    private void handleJoin(String[] tokens) {
        if(tokens.length>1){
            String topic = tokens[1];
            topicSet.add(topic);
        }
    }

    public boolean isMemberOfTopic(String topic){
        return topicSet.contains(topic);
    }

    private void handleMessage(String[] tokens) throws IOException {
        String sendTo = tokens[1];
        String body = tokens[2];
        boolean isTopic=sendTo.charAt(0) == '#';

        List<ServerWorker> workerList = server.getWorkerList();
        for(ServerWorker worker : workerList) {
            if(isTopic){
                if(worker.isMemberOfTopic(sendTo)){
                    String outMsg = "msg " + sendTo + ":" + login + " " + body + "\n";
                }
            } else{
                if (sendTo.equalsIgnoreCase(worker.getLogin())){
                    String outMsg = "msg " + login + " " + body + "\n";
                    worker.send(outMsg);
                }
            }
        }


    }

    private void handleLogoff() throws IOException {
        List<ServerWorker> workerList = server.getWorkerList();

        //String onlineMsg = "offline " + login + "\n";
        for (ServerWorker worker : workerList){
            if(!login.equals(worker.getLogin())){
                worker.send("offline " + worker.getLogin() + "\n");
            }
        }
        socket.close();
    }

    public String getLogin(){
        return login;
    }

    private void handleLogin(OutputStream outputStream, String[] tokens) throws IOException {
        if (tokens.length == 3){
            String login = tokens[1];
            String password = tokens[2];

            if((login.equals("guest") && password.equals("guest")) || (login.equals("filip") && password.equals("filip"))){
                String msg = "ok login\n";
                outputStream.write(msg.getBytes());
                this.login = login;
                System.out.println("User logged in succesfully: " + login);


                List<ServerWorker> workerList = server.getWorkerList();

                for(ServerWorker worker : workerList){
                        if (worker.getLogin()!=null){
                            if(!login.equals(worker.getLogin())){
                            String msg2 = "online " + worker.getLogin() + "\n";
                            send("online " + worker.getLogin() + "\n");
                            }
                        }
                }

                String onlineMsg = "online " + login + "\n";
                for(ServerWorker worker : workerList){
                    if(!login.equals(worker.getLogin())){
                        worker.send(onlineMsg);
                    }
                }

            } else {
                String msg = "error login\n";
                outputStream.write(msg.getBytes());
                System.err.println("Login failed for " + login);
            }
        }
    }

    private void send(String msg) throws IOException {
        if(login != null){
        outputStream.write(msg.getBytes());
        }
    }
}
