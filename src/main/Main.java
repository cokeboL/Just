 package main;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ��NIOʵ�ֵ�Echo Server��
 * @author NieYong <aofengblog@163.com>
 */
public class Main {
    
    //private final static Logger logger = Logger.getLogger(NioEchoServer.class.getName());
    
    // ���з�
    public final static char CR = '\r';
    
    // �س���
    public final static char LF = '\n';
    
    /**
     * @return ��ǰϵͳ���н�����
     */
    private static String getLineEnd() {
        return System.getProperty("line.separator");
    }
    
    /**
     * ���û�����״̬��־λ��position����Ϊ0��limit����Ϊcapacity��ֵ������mark��Ч��
     * ע��������ԭ�������ݻ��ڣ���û�������
     * 
     * @param buffer �ֽڻ�����
     */
    private static void clear(ByteBuffer buffer) {
        if (null != buffer) {
            buffer.clear();
        }
    }
    
    /**
     * ���ֽڻ�������ÿһ���ֽ�ת����ASCII�ַ���
     * @param buffer �ֽڻ�����
     * @return ת������ֽ������ַ���
     */
    private static String toDisplayChar(ByteBuffer buffer) {
        if (null == buffer) {
            return "null";
        }
        
        return Arrays.toString(buffer.array());
    }
    
    /**
     * ���ֽڻ�������utf8���룬ת�����ַ�����
     * 
     * @param buffer���ֽڻ�����
     * @return utf8����ת�����ַ���
     * @throws UnsupportedEncodingException
     */
    private static String convert2String(ByteBuffer buffer) throws UnsupportedEncodingException {
        return new String(buffer.array(), "utf8");
    }
    
    /**
     * ȥ��βĩ���н�������\r\n������ת�����ַ�����
     * 
     * @param buffer �ֽڻ�����
     * @return ����ȥ���н���������ַ�����
     * @throws UnsupportedEncodingException
     * @see #convert2String(ByteBuffer)
     */
    private static String getLineContent(ByteBuffer buffer) throws UnsupportedEncodingException {
        if (null == buffer) {
            return null;
        }
        
        byte[] result = new byte[buffer.limit()-2];
        System.arraycopy(buffer.array(), 0, result, 0, result.length);
        return convert2String(ByteBuffer.wrap(result));
    }
    
    /**
     * ˳��ϲ�����{@link ByteBuffer}�����ݣ��Ҳ��ı�{@link ByteBuffer}ԭ���ı�־λ������
     * <pre>
     * �ϲ����ByteBuffer = first + second
     * </pre>
     * @param first ��һ�����ϲ���{@link ByteBuffer}���ϲ�����������ǰ��
     * @param second �ڶ������ϲ���{@link ByteBuffer}���ϲ����������ں���
     * @return �ϲ�������ݡ��������{@link ByteBuffer}��Ϊnull������null��
     */
    private static ByteBuffer merge(ByteBuffer first, ByteBuffer second) {
        if (null == first && null == second) {
            return null;
        }
        
        int oneSize = null != first ? first.limit() : 0;
        int twoSize = null != second ? second.limit() : 0;
        ByteBuffer result = ByteBuffer.allocate(oneSize+twoSize);
        if (null != first) {
            result.put(Arrays.copyOfRange(first.array(), 0, oneSize));
        }
        if (null != second) {
            result.put(Arrays.copyOfRange(second.array(), 0, twoSize));
        }
        result.rewind();
        
        return result;
    }
    
    /**
     * ���ֽڻ������л�ȡ"һ��"������ȡ�����н���������ǰ������ݡ�
     * 
     * @param buffer ���뻺����
     * @return �������н����������ذ����н��������ڵ��ֽڻ����������򷵻�null��
     */
    private static ByteBuffer getLine(ByteBuffer buffer) {
        int index = 0;
        boolean findCR = false;
        int len = buffer.limit();
        while(index < len) {
            index ++;
            
            byte temp = buffer.get();
            if (CR == temp) {
                findCR = true;
            }
            if (LF == temp && findCR && index > 0) { // �ҵ����н�����
                byte[] copy = new byte[index];
                System.arraycopy(buffer.array(), 0, copy, 0, index);
                buffer.rewind(); // λ�ø�ԭ
                return ByteBuffer.wrap(copy);
            }
        }
        buffer.rewind(); // λ�ø�ԭ
        
        return null;
    }
    
    private static void readData(Selector selector, SelectionKey selectionKey) throws IOException {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        
        // ��ȡ�ϴ��Ѿ���ȡ������
        ByteBuffer oldBuffer = (ByteBuffer) selectionKey.attachment();
        System.out.println("��һ�ζ�ȡ�����ݣ�"+oldBuffer+getLineEnd()+toDisplayChar(oldBuffer));
        
        // ���µ�����
        int readNum = 0;
        ByteBuffer newBuffer = ByteBuffer.allocate(1024);
        if ( (readNum = socketChannel.read(newBuffer)) <= 0 ) {
            return;
        }
        
        System.out.println("��ζ�ȡ�����ݣ�"+newBuffer+getLineEnd()+toDisplayChar(newBuffer));
        
        
        newBuffer.flip();
        ByteBuffer lineRemain = getLine(newBuffer);
        
        System.out.println("������������ʣ�ಿ�֣�"+lineRemain+getLineEnd()+toDisplayChar(lineRemain));
        
        if (null != lineRemain) { // ��ȡ���н�����
            ByteBuffer completeLine = merge(oldBuffer, lineRemain);
            
            System.out.println("׼����������ݣ�"+completeLine+getLineEnd()+toDisplayChar(completeLine));
            
            while (completeLine.hasRemaining()) { // �п���һ��û��д�꣬����д
                socketChannel.write(completeLine);
            }
            
            // �������
            selectionKey.attach(null);
            clear(oldBuffer);
            clear(lineRemain);
            
            // �ж��Ƿ��˳�
            String lineStr = getLineContent(completeLine);
            
            System.out.println("�ж��Ƿ��˳��������ݣ�"+lineStr);
            
            if ("exit".equalsIgnoreCase(lineStr) || "quit".equalsIgnoreCase(lineStr)) {
                socketChannel.close();
            }
            
            // FIXME �н����������Ƿ������ݣ� �˲��ִ�����δ����
            if (lineRemain.limit()+2 < newBuffer.limit()) {
                byte[] temp = new byte[newBuffer.limit() - lineRemain.limit()];
                newBuffer.get(temp, lineRemain.limit(), temp.length);
                
                selectionKey.attach(temp);
            }
        } else { // û�ж���һ���������У����������Ҵ����Ѿ���ȡ�Ĳ�������
            ByteBuffer temp = merge(oldBuffer, newBuffer);
            socketChannel.register(selector, SelectionKey.OP_READ, temp); 
            
            System.out.println("�ݴ浽SelectionKey�����ݣ�"+temp+getLineEnd()+toDisplayChar(temp));
            
        }
    }

    /**
     * �����µ�Socket���ӡ�
     * 
     * @param selector ѡ����
     * @param selectionKey 
     * @return
     * @throws IOException
     * @throws ClosedChannelException
     */
    private static SocketChannel acceptNew(Selector selector,
            SelectionKey selectionKey) throws IOException,
            ClosedChannelException {
        ServerSocketChannel server = (ServerSocketChannel) selectionKey.channel();
        SocketChannel socketChannel = server.accept();
        if (null != socketChannel) {
            System.out.println("�յ�һ���µ����ӣ��ͻ���IP��"+socketChannel.socket().getInetAddress().getHostAddress()+"���ͻ���Port��"+socketChannel.socket().getPort());
            
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_READ);
        }
        
        return socketChannel;
    }
    
    /**
     * ������������
     * 
     * @param port ��������Ķ˿�
     * @param selectTimeout {@link Selector}���ͨ������״̬�ĳ�ʱʱ�䣨��λ�����룩
     */
    private static void startServer(int port, int selectTimeout) {
        ServerSocketChannel serverChannel = null;
        try {
            serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            ServerSocket serverSocket = serverChannel.socket();
            serverSocket.bind(new InetSocketAddress(port));
            
            System.out.println("NIO echo�������������ϣ������˿ڣ�" +port);
            
            
            Selector selector = Selector.open();
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            
            while (true) {
                int selectNum = selector.select(selectTimeout);
                if (0 == selectNum) {
                    continue;
                }
                
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> it = selectionKeys.iterator();
                while (it.hasNext()) {
                    SelectionKey selectionKey = (SelectionKey) it.next();
                    
                    // �����µ�Socket����
                    if (selectionKey.isAcceptable()) {
                        acceptNew(selector, selectionKey);
                    }
                    
                    // ��ȡ������Socket������
                    if (selectionKey.isReadable()) {
                        readData(selector, selectionKey);
                    }
                    
                    it.remove();
                } // end of while iterator
            }
        } catch (IOException e) {
        	System.out.println("�����������ӳ���"+e.toString());
        }
    }
    
    public static void main(String[] args) {
        int port = 19999;
        int selectTimeout = 1000;
        
        startServer(port, selectTimeout);
    }

}