package io.netty.example.mynio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.Set;

public class NioTest {
    public static void main(String[] args) throws IOException {
        int[] port = new int[5];
        port[0] = 5000;
        port[1] = 5001;
        port[2] = 5002;
        port[3] = 5003;
        port[4] = 5004;

        // 抽象类，不能new的。并且继承了Closeable，资源使用完可以自动关闭
        Selector selector = Selector.open();

        for (int i = 0; i < port.length; i++) {
            // 这个创建方式与Selector类似，这个时候serverSocketChannel还不能accept，它要进行先bind才可以，
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            // 设置非阻塞
            serverSocketChannel.configureBlocking(false);
            // 获取与ServerSocketChannel绑定的serverSocket
            ServerSocket serverSocket = serverSocketChannel.socket();
            InetSocketAddress address = new InetSocketAddress(port[i]);
            // 此时进行绑定
            serverSocket.bind(address);
            // 将channel注册给给定的selector上，返回SelectionKey。
            // 通过也可以根据返回SelectionKey获取给定的Channel
            SelectionKey selectionKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
//            SelectableChannel channel = selectionKey.channel();
        }
        // 这个读取的byteBuffer不写到循环里面是因为每每一次事件都要新建一个，没有必要。通过flip()来控制即可
        ByteBuffer byteBuffer = ByteBuffer.allocate(512);
        // 接收客户端请求
        while (true) {
            int count = selector.select();
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                if (key.isAcceptable()) {
                    ServerSocketChannel serverSocketChannel1 = (ServerSocketChannel) key.channel();
                    SocketChannel socketChannel = serverSocketChannel1.accept();
                    socketChannel.configureBlocking(false);
                    socketChannel.register(selector, SelectionKey.OP_READ);
                    iterator.remove();
                    System.out.println("来自客户端的连接：" + socketChannel);
                } else if (key.isReadable()) {
                    SocketChannel channel = (SocketChannel) key.channel();

                    int byteRead = 0;
                    while (true) {
                        byteBuffer.clear();
                        int read = channel.read(byteBuffer);
                        if (read <= 0) {
                            break;
                        }
                        byteBuffer.flip();
                        channel.write(byteBuffer);
                        byteRead += read;
                    }
                    iterator.remove();
                    System.out.println(channel + "，接收到数量为：" + byteRead);
                    //这个是错误的写法
//                    ByteBuffer byteBuffer = ByteBuffer.allocate(512);
//                    int read = channel.read(byteBuffer);
//                    System.out.println(channel + "byteBuffer"  + byteBuffer  +"，接收到数量为：" + read);
//
//                    byteBuffer.flip();
//                    channel.write(byteBuffer);
//                    iterator.remove();
                }
            }
        }
    }
}
