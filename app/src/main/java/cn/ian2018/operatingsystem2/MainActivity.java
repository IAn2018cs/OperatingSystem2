package cn.ian2018.operatingsystem2;

import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 陈帅 on 2017/4/21/024.
 */

public class MainActivity extends AppCompatActivity {

    private ImageView iv_about;
    private TextView tv_running_process;
    private ProgressBar process;
    private ImageView iv_idle;
    private ListView lv_ready;
    private ListView lv_block;
    private EditText et_name;
    private EditText et_input;
    private Button bt_add;
    private TextView tv_running_code;
    private TextView tv_running_results;
    private ListView lv_results;

    private List<PCB> readyQueue;
    private List<PCB> blockQueue;
    private List<PCB> resultsQueue;

    private boolean isRunning = false;

    private PCB runningProcess;

    private int processNum = 0;
    private int id = 0;

    private String[] code;
    private ReadyAdapter readyAdapter;
    private BlockAdapter blockAdapter;
    private ResultsAdapter resultsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化控件
        initView();

        // 初始化点击事件
        initClick();

        // 初始化就绪队列
        initReadyQueue();

        // 初始化阻塞队列
        initBlockQueue();

        // 初始化结果队列
        initResults();

        // cpu函数
        cpu();

        // 显示提示对话框
        showWarning();
    }

    // 0   1   2   3
    //s=9;s++;s--;end;
    // 0   1   2   3  4   5   6
    //s=9;s++;s--;!8;s++;s--;end;
    // cpu函数
    private void cpu() {
        // 开一个子线程 一直循环
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    while (true) {
                        // 每隔一秒执行一次
                        Thread.sleep(1000);

                        // 检查当前是否是空闲状态
                        checkIdle();

                        // 检查就绪队列
                        checkReady();

                        // 运行进程代码
                        runningCode();

                        // 检查阻塞队列
                        checkBlock();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // 检查当前是否是空闲状态
            private void checkIdle() {
                if (readyQueue.isEmpty() && blockQueue.isEmpty() && !isRunning) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Logs.d("cpu空闲状态");
                            iv_idle.setVisibility(View.VISIBLE);
                            process.setVisibility(View.GONE);
                        }
                    });
                }
            }

            // 检查就绪队列
            private void checkReady() {
                // 如果当前就绪队列不为空 并且没有正在执行的进程 就从就绪队列中取出一个进程执行
                if (!readyQueue.isEmpty() && !isRunning) {
                    Logs.d("从就绪队列中取出一个进程运行");
                    runningProcess = readyQueue.get(0);
                    readyQueue.remove(0);

                    // 更新执行状态
                    isRunning = true;

                    // 根据分号将字符串分割成字符串数组 方便解析代码
                    code = runningProcess.IR.split(";");
                    if (!code[code.length-1].equals("end")) {
                        Logs.d("没有加end语句");
                        runningProcess.IR = runningProcess.IR + ";end";
                        code = runningProcess.IR.split(";");
//                        erroCode();
                    }
                    Logs.d("数组长度："+code.length);

                     // 更新界面
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (runningProcess != null) {
                                tv_running_process.setText("id：" + runningProcess.id + "\t" + runningProcess.name + "进程");
                            }

                            readyAdapter.notifyDataSetChanged();

                            iv_idle.setVisibility(View.GONE);
                            process.setVisibility(View.VISIBLE);
                        }
                    });

                    if (code.length <= 0) {
                        erroCode();
                    }
                }
            }

            // 运行进程代码
            private void runningCode() {
                // 当前正在执行程序  进行代码的解析操作
                if (runningProcess != null && runningProcess.PC < code.length) {
                    Logs.d("解析并执行代码");
                    Logs.d("执行第" + runningProcess.PC + "行代码");
                    // 解析代码
                    exeCode(code[runningProcess.PC]);
                    // 更新界面
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (runningProcess != null && runningProcess.PC < code.length) {
                                tv_running_code.setText(code[runningProcess.PC]);
                                tv_running_results.setText(code[0].charAt(0) + "=" + runningProcess.variables);
                            }
                        }
                    });
                }
            }

            // 检查阻塞队列
            private void checkBlock() {
                // 如果阻塞队列不为空 就一直更新阻塞队列里的阻塞时间
                if (!blockQueue.isEmpty()) {
                    Logs.d("阻塞队列不为空，一直检索更新阻塞队列中的时间");
                    for (int i = 0; i < blockQueue.size(); i++) {
                        blockQueue.get(i).time--;

                        // 更新界面
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                blockAdapter.notifyDataSetChanged();
                            }
                        });

                        // 当一个进程的阻塞时间小于零后  将该进程唤醒
                        if (blockQueue.get(i).time < 0) {
                            Logs.d("第" + i + "个进程时间到，id为" + blockQueue.get(i).id);
                            wakeup(i);
                        }
                    }
                }
            }

            // 解析代码
            private void exeCode(String s) {
                // 结束语句
                if (s.equals("end")) {
                    Logs.d("运行到程序结束");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // 将结果添加到结果队列里
                            resultsQueue.add(runningProcess);
                            resultsAdapter.notifyDataSetChanged();

                            // 更新界面
                            tv_running_process.setText("");
                            tv_running_code.setText("");
                            tv_running_results.setText("");

                            // 销毁原语
                            distroy();
                        }
                    });
                // 阻塞语句
                } else if (s.length() > 0 && s.charAt(0) == '!') {
                    // 给PCB的阻塞时间赋值
                    runningProcess.time = Integer.valueOf(s.substring(1));
                    Logs.d("进程阻塞" + "，时间为" + runningProcess.time + "  中间变量为" + runningProcess.variables);
                    // 阻塞原语
                    block();
                } else {
                    int st = -1;
                    if (s.contains("=")) {
                        st = 0;
                    } else if (s.contains("++")) {
                        st = 1;
                    } else if (s.contains("--")){
                        st = 2;
                    }
                    switch (st) {
                        case 0:
                            try {
                                runningProcess.variables = Integer.valueOf(s.substring(s.indexOf("=")+1));
                            } catch (NumberFormatException e) {
                                erroCode();
                            }
                            break;
                        case 1:
                            runningProcess.variables = runningProcess.variables + 1;
                            break;
                        case 2:
                            runningProcess.variables = runningProcess.variables - 1;
                            break;
                        // 在输入非法语句时执行
                        default:
                            erroCode();
                    }
                }
                // 更新PC程序指针 下次取下一条指令
                if (runningProcess != null) {
                    runningProcess.PC++;
                }
            }

            // 输入进程代码有误
            private void erroCode() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        runningProcess.variables = -1024;
                        resultsQueue.add(runningProcess);
                        resultsAdapter.notifyDataSetChanged();

                        tv_running_process.setText("");
                        tv_running_code.setText("");
                        tv_running_results.setText("");

                        distroy();

                        Toast.makeText(getApplicationContext(), "输入进程内容有误，请重新输入", Toast.LENGTH_SHORT).show();
                    }
                });
            }

        }.start();
    }

    // 创建原语
    private void create(String name, String IR) {
        // 创建进程控制块，并初始化
        PCB pcb = new PCB();
        pcb.id = id;
        pcb.name = name;
        pcb.IR = IR;
        pcb.variables = 0;
        pcb.PSW = PCB.STATUS_READY;
        pcb.time = 0;
        pcb.PC = 0;

        // 插入到就绪队列
        readyQueue.add(pcb);

        // 刷新界面
        readyAdapter.notifyDataSetChanged();

        // 进程数加一
        processNum++;
        // id自增长
        id++;
    }

    // 终止原语
    private void distroy() {
        // 将正在运行标志更新
        isRunning = false;
        // 释放资源
        runningProcess = null;
        // 进程数减一
        processNum--;
    }

    // 阻塞原语
    private void block() {
        // 更改运行中的PCB状态
        runningProcess.PSW = PCB.STATUS_BLOCK;
        runningProcess.PC++;

        // 将其添加到阻塞队列中
        blockQueue.add(runningProcess);

        // 更新界面
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tv_running_process.setText("");
                blockAdapter.notifyDataSetChanged();
            }
        });

        // 更新执行状态
        isRunning = false;
        runningProcess = null;
    }

    // 唤醒原语
    private void wakeup(int i) {
        // 从阻塞队列中移除一个进程
        PCB pcb = blockQueue.get(i);
        pcb.PSW = PCB.STATUS_READY;
        blockQueue.remove(i);

        // 添加到就绪队列
        readyQueue.add(pcb);

        // 更新界面
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                blockAdapter.notifyDataSetChanged();
                readyAdapter.notifyDataSetChanged();
            }
        });
    }

    // 初始化就绪队列
    private void initReadyQueue() {
        readyQueue = new ArrayList<>();
        readyAdapter = new ReadyAdapter(this, readyQueue);
        lv_ready.setAdapter(readyAdapter);
    }

    // 初始化阻塞队列
    private void initBlockQueue() {
        blockQueue = new ArrayList<>();
        blockAdapter = new BlockAdapter(this, blockQueue);
        lv_block.setAdapter(blockAdapter);
    }

    // 初始化结果队列
    private void initResults() {
        resultsQueue = new ArrayList<>();
        resultsAdapter = new ResultsAdapter(this, resultsQueue);
        lv_results.setAdapter(resultsAdapter);
    }

    // 初始化点击事件
    private void initClick() {
        // 关于按钮点击事件
        iv_about.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("关于");
                builder.setIcon(R.mipmap.ic_launcher);
                builder.setMessage("这是基于Android系统构建的一个简单的模拟操作系统，模拟了操作系统的进程管理模块。\n由15软工的陈帅和田俊超开发完成。");
                builder.show();
            }
        });

        // 添加进程按钮点击事件
        bt_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = et_name.getText().toString().trim();
                String IR = et_input.getText().toString().trim();
                if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(IR)) {
                    if (processNum < 10) {
                        create(name, IR);

                        et_name.setText("");
                        et_input.setText("");
                    } else {
                        Toast.makeText(getApplicationContext(), "最多可运行10个进程", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "请将信息填写完整", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // 初始化控件
    private void initView() {
        iv_about = (ImageView) findViewById(R.id.iv_about);
        tv_running_process = (TextView) findViewById(R.id.tv_running_process);
        process = (ProgressBar) findViewById(R.id.process);
        iv_idle = (ImageView) findViewById(R.id.iv_idle);

        lv_ready = (ListView) findViewById(R.id.lv_ready);
        lv_block = (ListView) findViewById(R.id.lv_block);

        et_name = (EditText) findViewById(R.id.et_name);
        et_input = (EditText) findViewById(R.id.et_input);
        bt_add = (Button) findViewById(R.id.bt_add);

        tv_running_code = (TextView) findViewById(R.id.tv_running_code);

        tv_running_results = (TextView) findViewById(R.id.tv_running_results);

        lv_results = (ListView) findViewById(R.id.lv_results);
    }

    // 显示提示对话框
    private void showWarning() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.ic_warning);
        builder.setTitle("提示");
        builder.setMessage(R.string.warning_msg);
        builder.show();
    }
}
