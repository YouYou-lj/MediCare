#!/usr/bin/env python3
"""
MediCare 功能完整性测试脚本
对照 PR-001_MediCare_开发方案.md 验证所有模块功能
"""

import json
import sys
import time
from urllib.request import Request, urlopen
from urllib.error import HTTPError
from urllib.parse import quote

BASE = "http://localhost:8080/api"
COOKIE = None
RESULTS = []
# 存储测试中创建的资源ID，用于后续测试和清理
IDS = {}


def api(method, path, body=None, expect_code=200):
    """发送请求并返回响应"""
    global COOKIE
    url = f"{BASE}{path}"
    data = json.dumps(body).encode() if body else None
    req = Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if COOKIE:
        req.add_header("Cookie", COOKIE)
    try:
        resp = urlopen(req)
        if "Set-Cookie" in resp.headers:
            COOKIE = resp.headers["Set-Cookie"].split(";")[0]
        result = json.loads(resp.read().decode())
        ok = result.get("code") == expect_code
        return result, ok
    except HTTPError as e:
        try:
            result = json.loads(e.read().decode())
        except Exception:
            result = {"code": e.code, "message": str(e)}
        ok = result.get("code") == expect_code
        return result, ok


def test(module, name, result, ok, detail=""):
    """记录测试结果"""
    status = "PASS" if ok else "FAIL"
    code = result.get("code", "?")
    msg = result.get("message", "")
    extra = f" | {detail}" if detail else ""
    entry = {"module": module, "name": name, "status": status,
             "code": code, "message": msg, "detail": extra}
    RESULTS.append(entry)
    icon = "✅" if ok else "❌"
    print(f"  {icon} [{module}] {name}: code={code}{extra}")
    if not ok:
        print(f"      → {msg}")


# ============================================================
# 1. 认证模块
# ============================================================
def test_auth():
    global COOKIE
    print("\n📋 1. 认证模块")
    # 1.1 登录 - 正确密码
    r, ok = api("POST", "/auth/login", {"username": "admin", "password": "12345"})
    test("认证", "登录-正确密码", r, ok, f"user={r.get('data',{}).get('username','?')}")

    # 1.2 登录 - 错误密码
    r, ok = api("POST", "/auth/login", {"username": "admin", "password": "wrong"}, 400)
    test("认证", "登录-错误密码", r, ok)

    # 1.3 获取当前用户
    r, ok = api("GET", "/auth/current")
    test("认证", "获取当前用户", r, ok,
         f"role={r.get('data',{}).get('role','?')}" if ok else "")

    # 1.4 未登录访问受保护接口
    old_cookie = COOKIE
    COOKIE = "JSESSIONID=invalid"
    r, ok = api("GET", "/patients?page=1&size=5", expect_code=401)
    test("认证", "未登录访问受保护接口-401", r, ok)
    COOKIE = old_cookie


# ============================================================
# 2. Dashboard 统计模块
# ============================================================
def test_dashboard():
    print("\n📋 2. Dashboard 统计模块")
    r, ok = api("GET", "/dashboard/stats")
    if ok:
        d = r["data"]
        detail = f"reg={d.get('todayRegCount')}, wait={d.get('waitingCount')}, alert={d.get('stockAlertCount')}"
    else:
        detail = ""
    test("Dashboard", "获取统计数据", r, ok, detail)


# ============================================================
# 3. 患者管理模块 — PR: 登记/查询/搜索/编辑/删除
# ============================================================
def test_patient():
    print("\n📋 3. 患者管理模块")
    # 3.1 分页查询
    r, ok = api("GET", "/patients?page=1&size=5")
    total = r.get("data", {}).get("total", 0) if ok else 0
    test("患者", "分页查询", r, ok, f"total={total}")

    # 3.2 新增患者
    r, ok = api("POST", "/patients", {
        "idCard": f"320106{int(time.time())%100000000:08d}",
        "name": "测试患者",
        "gender": 1,
        "birthDate": "1999-01-01",
        "phone": "13800138000",
        "address": "南京市鼓楼区",
        "allergyInfo": "无"
    })
    if ok:
        IDS["patient"] = r["data"]["id"]
    test("患者", "新增患者", r, ok, f"id={IDS.get('patient')}")

    # 3.3 查询单个患者
    if IDS.get("patient"):
        r, ok = api("GET", f"/patients/{IDS['patient']}")
        test("患者", "查询单个患者", r, ok,
             f"name={r.get('data',{}).get('name','?')}" if ok else "")

    # 3.4 搜索患者
    r, ok = api("GET", f"/patients/search?keyword={quote('测试')}")
    test("患者", "搜索患者", r, ok,
         f"count={len(r.get('data',[]))}" if ok else "")

    # 3.5 编辑患者
    if IDS.get("patient"):
        r, ok = api("PUT", f"/patients/{IDS['patient']}", {
            "id": IDS["patient"],
            "idCard": f"320106{int(time.time())%100000000:08d}",
            "name": "测试患者-修改",
            "gender": 1,
            "phone": "13900139000",
            "address": "南京市玄武区"
        })
        test("患者", "编辑患者", r, ok,
             f"name={r.get('data',{}).get('name','?') if ok else ''}")

    # 3.6 身份证唯一性校验
    r, ok = api("POST", "/patients", {
        "idCard": "320106199901011234",
        "name": "重复患者",
        "gender": 0,
    }, expect_code=400)
    test("患者", "身份证唯一性校验", r, ok)


# ============================================================
# 4. 基础数据模块 — PR: 科室管理、医生管理、排班管理
# ============================================================
def test_basic_data():
    print("\n📋 4. 基础数据模块")
    # 4.1 科室列表
    r, ok = api("GET", "/departments")
    depts = r.get("data", []) if ok else []
    test("基础数据", "科室列表", r, ok, f"count={len(depts)}")
    if depts:
        IDS["department"] = depts[0]["id"]

    # 4.2 新增科室
    r, ok = api("POST", "/departments", {
        "name": "测试科室",
        "location": "门诊楼4层",
        "phone": "025-88880099"
    })
    if ok:
        IDS["new_dept"] = r["data"]["id"]
    test("基础数据", "新增科室", r, ok)

    # 4.3 编辑科室
    if IDS.get("new_dept"):
        r, ok = api("PUT", f"/departments/{IDS['new_dept']}", {
            "name": "测试科室-修改",
            "location": "门诊楼5层"
        })
        test("基础数据", "编辑科室", r, ok)

    # 4.4 医生列表
    r, ok = api("GET", "/doctors")
    doctors = r.get("data", []) if ok else []
    test("基础数据", "医生列表", r, ok, f"count={len(doctors)}")
    if doctors:
        IDS["doctor"] = doctors[0]["id"]
        IDS["doctor_dept"] = doctors[0].get("departmentId")

    # 4.5 按科室查医生
    if IDS.get("doctor_dept"):
        r, ok = api("GET", f"/doctors?deptId={IDS['doctor_dept']}")
        test("基础数据", "按科室查医生", r, ok,
             f"count={len(r.get('data',[]))}" if ok else "")

    # 4.6 排班查询
    today = time.strftime("%Y-%m-%d")
    if IDS.get("doctor_dept"):
        r, ok = api("GET", f"/schedules?date={today}&deptId={IDS['doctor_dept']}")
        schedules = r.get("data", []) if ok else []
        test("基础数据", "排班查询", r, ok, f"count={len(schedules)}")
        if schedules:
            IDS["schedule"] = schedules[0]["id"]


# ============================================================
# 5. 挂号预约模块 — PR: 科室选择→排班查看→现场挂号→叫号→完成→取消
# ============================================================
def test_registration():
    print("\n📋 5. 挂号预约模块")
    today = time.strftime("%Y-%m-%d")

    # 5.1 确保有排班数据（先创建排班）
    if not IDS.get("schedule") and IDS.get("doctor"):
        r, ok = api("POST", "/schedules", {
            "doctorId": IDS["doctor"],
            "workDate": today,
            "timeSlot": "上午",
            "totalSlots": 20,
            "remainSlots": 20
        })
        if ok:
            IDS["schedule"] = r["data"]["id"]

    # 5.2 今日挂号列表
    r, ok = api("GET", f"/registrations?date={today}")
    test("挂号", "今日挂号列表", r, ok,
         f"count={len(r.get('data',[]))}" if ok else "")

    # 5.3 现场挂号
    if IDS.get("patient") and IDS.get("schedule"):
        r, ok = api("POST", "/registrations", {
            "patientId": IDS["patient"],
            "scheduleId": IDS["schedule"]
        })
        if ok:
            IDS["registration"] = r["data"]["id"]
        test("挂号", "现场挂号", r, ok,
             f"regId={IDS.get('registration')}")
    else:
        print("  ⚠️ 跳过挂号：缺少患者或排班数据")

    # 5.4 叫号 (状态 0→1)
    if IDS.get("registration"):
        r, ok = api("PUT", f"/registrations/{IDS['registration']}/call")
        test("挂号", "叫号(0→1)", r, ok)

    # 5.5 完成就诊 (状态 1→2)
    if IDS.get("registration"):
        r, ok = api("PUT", f"/registrations/{IDS['registration']}/complete")
        test("挂号", "完成就诊(1→2)", r, ok)


# ============================================================
# 6. 医生工作站/病历模块 — PR: 病历书写、病历检索、详情查看
# ============================================================
def test_medical_record():
    print("\n📋 6. 病历管理模块")
    # 6.1 创建病历
    if IDS.get("registration") and IDS.get("patient") and IDS.get("doctor"):
        r, ok = api("POST", "/medical-records", {
            "registrationId": IDS["registration"],
            "patientId": IDS["patient"],
            "doctorId": IDS["doctor"],
            "chiefComplaint": "头痛三天",
            "presentIllness": "反复头痛，无恶心呕吐",
            "pastHistory": "高血压5年",
            "physicalExam": "血压150/95mmHg",
            "diagnosis": "原发性高血压",
            "advice": "降压治疗，定期复查"
        })
        if ok:
            IDS["medical_record"] = r["data"]["id"]
        test("病历", "创建病历", r, ok, f"id={IDS.get('medical_record')}")
    else:
        print("  ⚠️ 跳过创建病历：缺少挂号/患者/医生数据")

    # 6.2 按患者查询病历
    if IDS.get("patient"):
        r, ok = api("GET", f"/medical-records?patientId={IDS['patient']}")
        test("病历", "按患者查询病历列表", r, ok,
             f"count={len(r.get('data',[]))}" if ok else "")

    # 6.3 按挂号ID查询病历
    if IDS.get("registration"):
        r, ok = api("GET", f"/medical-records/by-registration/{IDS['registration']}")
        test("病历", "按挂号ID查询病历", r, ok,
             f"diagnosis={r.get('data',{}).get('diagnosis','?')}" if ok else "")

    # 6.4 查询病历详情
    if IDS.get("medical_record"):
        r, ok = api("GET", f"/medical-records/{IDS['medical_record']}")
        test("病历", "查询病历详情(/{id})", r, ok)

    # 6.5 编辑病历
    if IDS.get("medical_record"):
        r, ok = api("PUT", f"/medical-records/{IDS['medical_record']}", {
            "registrationId": IDS["registration"],
            "patientId": IDS["patient"],
            "doctorId": IDS["doctor"],
            "chiefComplaint": "头痛三天-修改",
            "diagnosis": "高血压2级"
        })
        test("病历", "编辑病历", r, ok)

    # 6.6 同一挂号重复创建病历应失败
    if IDS.get("registration") and IDS.get("patient") and IDS.get("doctor"):
        r, ok = api("POST", "/medical-records", {
            "registrationId": IDS["registration"],
            "patientId": IDS["patient"],
            "doctorId": IDS["doctor"],
            "chiefComplaint": "重复"
        }, expect_code=400)
        test("病历", "重复创建病历-应拒绝", r, ok)


# ============================================================
# 7. 处方管理模块 — PR: 处方开立、库存校验、发药、作废
# ============================================================
def test_prescription():
    print("\n📋 7. 处方管理模块")
    # 7.1 获取药品列表(为处方准备药品ID)
    r, ok = api("GET", "/medicines")
    medicines = r.get("data", []) if ok else []
    if medicines:
        IDS["medicine"] = medicines[0]["id"]
        IDS["medicine2"] = medicines[1]["id"] if len(medicines) > 1 else None

    # 7.2 创建处方
    if IDS.get("medical_record") and IDS.get("patient") and IDS.get("doctor") and IDS.get("medicine"):
        items = [{"medicineId": IDS["medicine"], "quantity": 2,
                  "dosage": "一日三次", "usageDesc": "饭后服用",
                  "unitPrice": 12.50}]
        if IDS.get("medicine2"):
            items.append({"medicineId": IDS["medicine2"], "quantity": 1,
                          "dosage": "一日一次", "usageDesc": "睡前服用",
                          "unitPrice": 18.00})
        r, ok = api("POST", "/prescriptions", {
            "prescription": {
                "recordId": IDS["medical_record"],
                "patientId": IDS["patient"],
                "doctorId": IDS["doctor"]
            },
            "items": items
        })
        if ok:
            IDS["prescription"] = r["data"]["id"]
        test("处方", "创建处方", r, ok, f"id={IDS.get('prescription')}")
    else:
        print("  ⚠️ 跳过创建处方：缺少病历/药品数据")

    # 7.3 按患者查询处方列表
    if IDS.get("patient"):
        r, ok = api("GET", f"/prescriptions?patientId={IDS['patient']}")
        test("处方", "按患者查询处方列表", r, ok,
             f"count={len(r.get('data',[]))}" if ok else "")

    # 7.4 查询处方详情
    if IDS.get("prescription"):
        r, ok = api("GET", f"/prescriptions/{IDS['prescription']}")
        test("处方", "查询处方详情", r, ok)

    # 7.5 按病历查询处方
    if IDS.get("medical_record"):
        r, ok = api("GET", f"/prescriptions/by-record/{IDS['medical_record']}")
        test("处方", "按病历查询处方", r, ok)


# ============================================================
# 8. 药品库存模块 — PR: 入库、出库、库存查询、有效期预警
# ============================================================
def test_pharmacy():
    print("\n📋 8. 药品库存模块")
    # 8.1 药品列表
    r, ok = api("GET", "/medicines")
    test("药品", "药品列表", r, ok,
         f"count={len(r.get('data',[]))}" if ok else "")

    # 8.2 搜索药品
    r, ok = api("GET", f"/medicines?keyword={quote('阿莫西林')}")
    test("药品", "搜索药品", r, ok,
         f"count={len(r.get('data',[]))}" if ok else "")

    # 8.3 查询单个药品
    if IDS.get("medicine"):
        r, ok = api("GET", f"/medicines/{IDS['medicine']}")
        test("药品", "查询单个药品", r, ok,
             f"stock={r.get('data',{}).get('stock','?')}" if ok else "")

    # 8.4 入库
    if IDS.get("medicine"):
        r, ok = api("POST", f"/medicines/{IDS['medicine']}/stock-in", {
            "quantity": 100,
            "batchNo": "B20260601",
            "expiryDate": "2027-06-01",
            "operator": "admin",
            "remark": "测试入库"
        })
        test("药品", "入库", r, ok)

    # 8.5 出库
    if IDS.get("medicine"):
        r, ok = api("POST", f"/medicines/{IDS['medicine']}/stock-out", {
            "quantity": 10,
            "batchNo": "B20260601",
            "expiryDate": "2027-06-01",
            "operator": "admin",
            "remark": "测试出库"
        })
        test("药品", "出库", r, ok)

    # 8.6 库存不足出库应失败
    if IDS.get("medicine"):
        r, ok = api("POST", f"/medicines/{IDS['medicine']}/stock-out", {
            "quantity": 99999,
            "operator": "admin"
        }, expect_code=400)
        test("药品", "库存不足出库-应拒绝", r, ok)

    # 8.7 低库存预警
    r, ok = api("GET", "/medicines/low-stock")
    test("药品", "低库存预警列表", r, ok,
         f"count={len(r.get('data',[]))}" if ok else "")

    # 8.8 库存日志
    if IDS.get("medicine"):
        r, ok = api("GET", f"/prescriptions/inventory-logs?medicineId={IDS['medicine']}")
        test("药品", "库存变动日志", r, ok,
             f"count={len(r.get('data',[]))}" if ok else "")

    # 8.9 新增药品
    r, ok = api("POST", "/medicines", {
        "name": "测试药品",
        "spec": "10mg*30片",
        "unit": "盒",
        "stock": 200,
        "safetyStock": 20,
        "pinyinCode": "CSYP",
        "price": 25.00,
        "manufacturer": "测试药厂",
        "status": 1
    })
    if ok:
        IDS["new_medicine"] = r["data"]["id"]
    test("药品", "新增药品", r, ok)

    # 8.10 编辑药品
    if IDS.get("new_medicine"):
        r, ok = api("PUT", f"/medicines/{IDS['new_medicine']}", {
            "name": "测试药品-修改",
            "spec": "10mg*30片",
            "unit": "盒",
            "stock": 200,
            "safetyStock": 30,
            "price": 28.00,
            "status": 1
        })
        test("药品", "编辑药品", r, ok)


# ============================================================
# 9. 系统设置模块 — PR: 管理员管理、密码修改
# ============================================================
def test_settings():
    print("\n📋 9. 系统设置模块")
    # 9.1 用户列表
    r, ok = api("GET", "/users")
    test("设置", "用户列表", r, ok,
         f"count={len(r.get('data',[]))}" if ok else "")

    # 9.2 新增用户
    r, ok = api("POST", "/users", {
        "username": "testdoctor",
        "password": "123456",
        "realName": "测试医生",
        "role": "doctor",
        "status": 1
    })
    if ok:
        IDS["test_user"] = r["data"]["id"]
    test("设置", "新增用户", r, ok)

    # 9.3 修改密码 (admin改自己的密码)
    r, ok = api("PUT", "/users/1/password", {
        "oldPassword": "12345",
        "newPassword": "12345"
    })
    test("设置", "修改密码", r, ok)


# ============================================================
# 10. 角色权限校验 — PR: admin/doctor/pharmacist 角色隔离
# ============================================================
def test_role_auth():
    global COOKIE
    print("\n📋 10. 角色权限校验")
    # 当前是 admin，创建一个无关联的临时患者用于删除测试
    r, _ = api("POST", "/patients", {
        "idCard": f"320106{int(time.time())%100000000:08d}",
        "name": "临时患者-待删除",
        "gender": 0,
        "phone": "13700000001"
    })
    temp_patient_id = r.get("data", {}).get("id") if r.get("code") == 200 else None
    if temp_patient_id:
        r, ok = api("DELETE", f"/patients/{temp_patient_id}")
        test("权限", "admin删除患者", r, ok)
    else:
        test("权限", "admin删除患者", r, False, "创建临时患者失败")

    # 登录为 doctor 角色（如果有doctor用户）
    # 先检查是否有doctor用户
    r, _ = api("GET", "/users")
    users = r.get("data", [])
    doctor_user = None
    for u in users:
        if u.get("role") == "doctor":
            doctor_user = u
            break

    if doctor_user:
        # doctor 尝试删除患者应被拒绝
        # 先创建一个临时患者用于删除测试
        r2, _ = api("POST", "/patients", {
            "idCard": "320106200001011111",
            "name": "临时患者",
            "gender": 0,
            "phone": "13700000000"
        })
        temp_id = r2.get("data", {}).get("id") if r2.get("code") == 200 else None

        if temp_id:
            # 切换为doctor登录
            r, ok = api("POST", "/auth/login", {
                "username": doctor_user["username"],
                "password": "12345"
            })
            if ok:
                r, ok = api("DELETE", f"/patients/{temp_id}", expect_code=400)
                test("权限", "doctor删除患者-应拒绝", r, ok)

                # doctor 应能查询患者
                r, ok = api("GET", "/patients?page=1&size=5")
                test("权限", "doctor查询患者", r, ok)
            else:
                print("  ⚠️ doctor用户登录失败，跳过权限测试")

        # 切回admin
        api("POST", "/auth/login", {"username": "admin", "password": "12345"})
    else:
        print("  ⚠️ 无doctor用户，跳过角色权限测试")


# ============================================================
# 清理测试数据
# ============================================================
def cleanup():
    print("\n🧹 清理测试数据")
    # 切回 admin 确保有权限
    api("POST", "/auth/login", {"username": "admin", "password": "12345"})

    # 删除测试用户
    if IDS.get("test_user"):
        r, ok = api("DELETE", f"/users/{IDS['test_user']}")
        print(f"  删除测试用户: {'ok' if ok else 'fail'}")

    # 删除测试药品
    if IDS.get("new_medicine"):
        r, ok = api("DELETE", f"/medicines/{IDS['new_medicine']}")
        print(f"  删除测试药品: {'ok' if ok else 'fail'}")

    # 删除测试科室
    if IDS.get("new_dept"):
        r, ok = api("DELETE", f"/departments/{IDS['new_dept']}")
        print(f"  删除测试科室: {'ok' if ok else 'fail'}")


# ============================================================
# 报告
# ============================================================
def report():
    print("\n" + "=" * 70)
    print("📊 测试报告 — 对照 PR-001 功能完整性验证")
    print("=" * 70)

    modules = {}
    for r in RESULTS:
        m = r["module"]
        if m not in modules:
            modules[m] = {"pass": 0, "fail": 0, "items": []}
        if r["status"] == "PASS":
            modules[m]["pass"] += 1
        else:
            modules[m]["fail"] += 1
        modules[m]["items"].append(r)

    total_pass = total_fail = 0
    for m, v in modules.items():
        total_pass += v["pass"]
        total_fail += v["fail"]
        print(f"\n  [{m}] ✅ {v['pass']} ❌ {v['fail']}")
        for item in v["items"]:
            icon = "✅" if item["status"] == "PASS" else "❌"
            print(f"    {icon} {item['name']} (code={item['code']}){item['detail']}")

    print(f"\n{'─' * 70}")
    print(f"  总计: ✅ {total_pass}  ❌ {total_fail}  共 {total_pass + total_fail} 项")
    rate = total_pass / (total_pass + total_fail) * 100 if (total_pass + total_fail) else 0
    print(f"  通过率: {rate:.1f}%")

    if total_fail > 0:
        print(f"\n  ❌ 失败项:")
        for r in RESULTS:
            if r["status"] == "FAIL":
                print(f"    - [{r['module']}] {r['name']}: code={r['code']} {r['message']}")

    return total_fail


# ============================================================
# 主流程
# ============================================================
if __name__ == "__main__":
    print("=" * 70)
    print("🏥 MediCare 功能完整性测试")
    print("   对照: PR-001_MediCare_开发方案.md")
    print("=" * 70)

    try:
        test_auth()
        test_dashboard()
        test_patient()
        test_basic_data()
        test_registration()
        test_medical_record()
        test_prescription()
        test_pharmacy()
        test_settings()
        test_role_auth()
    except Exception as e:
        print(f"\n💥 测试执行异常: {e}")
        import traceback
        traceback.print_exc()
    finally:
        cleanup()
        fail_count = report()

    sys.exit(1 if fail_count > 0 else 0)
