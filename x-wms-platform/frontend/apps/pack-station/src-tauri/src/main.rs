// Prevents additional console window on Windows in release
#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

use serde::{Deserialize, Serialize};
use std::sync::Mutex;
use tauri::State;

// 应用状态：电子秤连接状态
struct AppState {
    scale_port: Mutex<Option<String>>,
    scale_baud_rate: Mutex<u32>,
    printer_name: Mutex<Option<String>>,
}

// 电子秤读数
#[derive(Serialize, Deserialize)]
struct ScaleReading {
    weight: f64,
    unit: String,
    stable: bool,
}

// 打印参数
#[derive(Serialize, Deserialize)]
struct PrintParams {
    printer_name: String,
    content: String,
    copies: Option<u32>,
}

// 连接电子秤
#[tauri::command]
fn connect_scale(port: String, baud_rate: u32, state: State<AppState>) -> Result<String, String> {
    *state.scale_port.lock().unwrap() = Some(port.clone());
    *state.scale_baud_rate.lock().unwrap() = baud_rate;
    Ok(format!("电子秤已连接: {} @ {}bps", port, baud_rate))
}

// 读取电子秤重量（模拟实现，实际需串口通信）
#[tauri::command]
fn read_scale(state: State<AppState>) -> Result<ScaleReading, String> {
    let port = state.scale_port.lock().unwrap();
    if port.is_none() {
        return Err("电子秤未连接".to_string());
    }
    // 模拟读数，实际应通过tokio-serial读取串口
    Ok(ScaleReading {
        weight: 2.350,
        unit: "kg".to_string(),
        stable: true,
    })
}

// 断开电子秤
#[tauri::command]
fn disconnect_scale(state: State<AppState>) -> Result<String, String> {
    *state.scale_port.lock().unwrap() = None;
    Ok("电子秤已断开".to_string())
}

// 设置打印机
#[tauri::command]
fn set_printer(name: String, state: State<AppState>) -> Result<String, String> {
    *state.printer_name.lock().unwrap() = Some(name.clone());
    Ok(format!("打印机已设置: {}", name))
}

// 打印快递单（ESC/POS指令，模拟实现）
#[tauri::command]
fn print_label(params: PrintParams, state: State<AppState>) -> Result<String, String> {
    let printer = state.printer_name.lock().unwrap();
    if printer.is_none() {
        return Err("打印机未设置".to_string());
    }
    // 实际应通过escpos库生成ESC/POS指令并发送到打印机
    let copies = params.copies.unwrap_or(1);
    Ok(format!("已发送打印任务到 {} ({}份)", params.printer_name, copies))
}

// 获取可用串口列表
#[tauri::command]
fn list_serial_ports() -> Result<Vec<String>, String> {
    // 实际应通过tokio-serial的available_ports()获取
    Ok(vec!["COM1".to_string(), "COM3".to_string(), "COM5".to_string()])
}

// 获取可用打印机列表
#[tauri::command]
fn list_printers() -> Result<Vec<String>, String> {
    // 实际应通过系统API获取
    Ok(vec!["热敏打印机-前台".to_string(), "激光打印机-办公".to_string()])
}

fn main() {
    tauri::Builder::default()
        .plugin(tauri_plugin_shell::init())
        .manage(AppState {
            scale_port: Mutex::new(None),
            scale_baud_rate: Mutex::new(9600),
            printer_name: Mutex::new(None),
        })
        .invoke_handler(tauri::generate_handler![
            connect_scale,
            read_scale,
            disconnect_scale,
            set_printer,
            print_label,
            list_serial_ports,
            list_printers,
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
