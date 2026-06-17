import type { MockMethod } from 'vite-plugin-mock'

const departments = [
  { id: 1, name: '内科', location: '门诊楼 1 层 A 区', phone: '025-88880001' },
  { id: 2, name: '外科', location: '门诊楼 1 层 B 区', phone: '025-88880002' },
  { id: 3, name: '儿科', location: '门诊楼 2 层 C 区', phone: '025-88880003' },
  { id: 4, name: '妇产科', location: '门诊楼 2 层 D 区', phone: '025-88880004' },
  { id: 5, name: '中医科', location: '门诊楼 3 层 E 区', phone: '025-88880005' },
]

const doctors = [
  { id: 1, name: '张伟', departmentId: 1, title: '主任医师', status: 1, departmentName: '内科' },
  { id: 2, name: '李娜', departmentId: 1, title: '副主任医师', status: 1, departmentName: '内科' },
  { id: 3, name: '王强', departmentId: 2, title: '主治医师', status: 1, departmentName: '外科' },
  { id: 4, name: '刘洋', departmentId: 3, title: '主任医师', status: 1, departmentName: '儿科' },
  { id: 5, name: '陈静', departmentId: 4, title: '副主任医师', status: 1, departmentName: '妇产科' },
  { id: 6, name: '赵敏', departmentId: 5, title: '医师', status: 1, departmentName: '中医科' },
]

const patients = [
  { id: 1, idCard: '320101199001011234', name: '王芳', gender: 0, birthDate: '1990-01-01', phone: '13800138001', address: '南京市玄武区', allergyInfo: '' },
  { id: 2, idCard: '320101198502153321', name: '李明', gender: 1, birthDate: '1985-02-15', phone: '13800138002', address: '南京市鼓楼区', allergyInfo: '青霉素过敏' },
  { id: 3, idCard: '320101197803206543', name: '张丽', gender: 0, birthDate: '1978-03-20', phone: '13800138003', address: '南京市建邺区', allergyInfo: '' },
  { id: 4, idCard: '320101199505108765', name: '刘强', gender: 1, birthDate: '1995-05-10', phone: '13800138004', address: '南京市秦淮区', allergyInfo: '磺胺类药物过敏' },
]

const today = new Date().toISOString().slice(0, 10)

const schedules = [
  { id: 1, doctorId: 1, workDate: today, timeSlot: '上午', totalSlots: 20, remainSlots: 15, doctorName: '张伟', departmentName: '内科' },
  { id: 2, doctorId: 1, workDate: today, timeSlot: '下午', totalSlots: 15, remainSlots: 10, doctorName: '张伟', departmentName: '内科' },
  { id: 3, doctorId: 2, workDate: today, timeSlot: '上午', totalSlots: 20, remainSlots: 18, doctorName: '李娜', departmentName: '内科' },
  { id: 4, doctorId: 3, workDate: today, timeSlot: '上午', totalSlots: 15, remainSlots: 12, doctorName: '王强', departmentName: '外科' },
  { id: 5, doctorId: 4, workDate: today, timeSlot: '下午', totalSlots: 10, remainSlots: 8, doctorName: '刘洋', departmentName: '儿科' },
]

const registrations = [
  { id: 1, patientId: 1, scheduleId: 1, doctorId: 1, regTime: `${today}T08:30:00`, status: 0, seqNo: 1, fee: 10.00, patientName: '王芳', doctorName: '张伟', departmentName: '内科', timeSlot: '上午' },
  { id: 2, patientId: 2, scheduleId: 1, doctorId: 1, regTime: `${today}T08:45:00`, status: 1, seqNo: 2, fee: 10.00, patientName: '李明', doctorName: '张伟', departmentName: '内科', timeSlot: '上午' },
  { id: 3, patientId: 3, scheduleId: 4, doctorId: 3, regTime: `${today}T09:00:00`, status: 2, seqNo: 1, fee: 10.00, patientName: '张丽', doctorName: '王强', departmentName: '外科', timeSlot: '上午' },
]

const medicines = [
  { id: 1, name: '阿莫西林胶囊', spec: '0.25g*24粒', unit: '盒', stock: 500, safetyStock: 50, expiryDate: '2027-06-30', batchNo: 'HB20240101', pinyinCode: 'AMXL', price: 12.50, manufacturer: '华北制药', status: 1 },
  { id: 2, name: '布洛芬缓释胶囊', spec: '0.3g*20粒', unit: '盒', stock: 300, safetyStock: 30, expiryDate: '2027-12-31', batchNo: 'FB20240201', pinyinCode: 'BLF', price: 18.00, manufacturer: '芬必得', status: 1 },
  { id: 3, name: '感冒清热颗粒', spec: '12g*10袋', unit: '盒', stock: 8, safetyStock: 20, expiryDate: '2027-03-15', batchNo: 'TRT20240101', pinyinCode: 'GMQRKL', price: 15.80, manufacturer: '同仁堂', status: 1 },
  { id: 4, name: '头孢克肟片', spec: '0.1g*6片', unit: '盒', stock: 150, safetyStock: 15, expiryDate: '2027-09-30', batchNo: 'BYS20240301', pinyinCode: 'TBKW', price: 28.50, manufacturer: '白云山', status: 1 },
  { id: 5, name: '维生素C片', spec: '0.1g*100片', unit: '瓶', stock: 100, safetyStock: 10, expiryDate: '2028-01-31', batchNo: 'DBZY20240101', pinyinCode: 'WSSC', price: 6.50, manufacturer: '东北制药', status: 1 },
]

const medicalRecords = [
  { id: 1, registrationId: 3, patientId: 3, doctorId: 3, chiefComplaint: '右下腹疼痛3天', presentIllness: '3天前无明显诱因出现右下腹疼痛，逐渐加重', pastHistory: '无特殊', physicalExam: '右下腹压痛阳性，反跳痛阳性', diagnosis: '急性阑尾炎', advice: '建议手术治疗', patientName: '张丽', doctorName: '王强' },
]

const prescriptions = [
  { id: 1, recordId: 1, patientId: 3, doctorId: 3, totalAmount: 47.00, status: 0, patientName: '张丽', doctorName: '王强', items: [
    { id: 1, prescriptionId: 1, medicineId: 1, quantity: 2, usageDesc: '每日3次，每次2粒', unitPrice: 12.50, amount: 25.00, medicineName: '阿莫西林胶囊', medicineSpec: '0.25g*24粒', medicineUnit: '盒' },
    { id: 2, prescriptionId: 1, medicineId: 2, quantity: 1, usageDesc: '每日2次，每次1粒', unitPrice: 18.00, amount: 18.00, medicineName: '布洛芬缓释胶囊', medicineSpec: '0.3g*20粒', medicineUnit: '盒' },
  ]},
]

const users = [
  { id: 1, username: 'admin', realName: '系统管理员', role: 'admin', status: 1, doctorId: null },
  { id: 2, username: 'doctor1', realName: '张伟', role: 'doctor', status: 1, doctorId: 1 },
  { id: 3, username: 'pharmacist1', realName: '药剂师王芳', role: 'pharmacist', status: 1, doctorId: null },
]

let nextId = 100

export default [
  // ========== Auth ==========
  {
    url: '/api/auth/login',
    method: 'post',
    response: ({ body }: any) => {
      const user = users.find((u) => u.username === body.username)
      if (!user) return { code: 401, message: '用户名或密码错误', data: null }
      return { code: 200, message: 'success', data: { user } }
    },
  },
  {
    url: '/api/auth/logout',
    method: 'post',
    response: () => ({ code: 200, message: 'success', data: null }),
  },
  {
    url: '/api/auth/current',
    method: 'get',
    response: () => ({ code: 200, message: 'success', data: users[0] }),
  },

  // ========== Dashboard ==========
  {
    url: '/api/dashboard/stats',
    method: 'get',
    response: () => ({ code: 200, message: 'success', data: { todayRegCount: 3, waitingCount: 1, stockAlertCount: 1 } }),
  },

  // ========== Departments ==========
  {
    url: '/api/departments',
    method: 'get',
    response: () => ({ code: 200, message: 'success', data: departments }),
  },
  {
    url: '/api/departments',
    method: 'post',
    response: ({ body }: any) => {
      const item = { id: ++nextId, ...body }
      departments.push(item as any)
      return { code: 200, message: 'success', data: item }
    },
  },
  {
    url: '/api/departments',
    method: 'put',
    response: ({ body }: any) => ({ code: 200, message: 'success', data: body }),
  },
  {
    url: '/api/departments',
    method: 'delete',
    response: () => ({ code: 200, message: 'success', data: null }),
  },

  // ========== Doctors ==========
  {
    url: '/api/doctors',
    method: 'get',
    response: ({ query }: any) => {
      let result = doctors
      if (query.deptId) result = result.filter((d) => d.departmentId === Number(query.deptId))
      return { code: 200, message: 'success', data: result }
    },
  },
  {
    url: '/api/doctors',
    method: 'post',
    response: ({ body }: any) => {
      const dept = departments.find((d) => d.id === body.departmentId)
      const item = { id: ++nextId, ...body, departmentName: dept?.name || '' }
      doctors.push(item as any)
      return { code: 200, message: 'success', data: item }
    },
  },
  {
    url: '/api/doctors',
    method: 'put',
    response: ({ body }: any) => ({ code: 200, message: 'success', data: body }),
  },
  {
    url: '/api/doctors',
    method: 'delete',
    response: () => ({ code: 200, message: 'success', data: null }),
  },

  // ========== Patients ==========
  {
    url: '/api/patients',
    method: 'get',
    response: () => ({ code: 200, message: 'success', data: { list: patients, total: patients.length, page: 1, size: 20 } }),
  },
  {
    url: '/api/patients/search',
    method: 'get',
    response: ({ query }: any) => {
      const kw = query.keyword?.toLowerCase() || ''
      const result = patients.filter((p) => p.name.toLowerCase().includes(kw) || p.idCard.includes(kw) || p.phone?.includes(kw))
      return { code: 200, message: 'success', data: result }
    },
  },
  {
    url: '/api/patients',
    method: 'post',
    response: ({ body }: any) => {
      const item = { id: ++nextId, ...body }
      patients.push(item as any)
      return { code: 200, message: 'success', data: item }
    },
  },
  {
    url: '/api/patients',
    method: 'put',
    response: ({ body }: any) => ({ code: 200, message: 'success', data: body }),
  },
  {
    url: '/api/patients',
    method: 'delete',
    response: () => ({ code: 200, message: 'success', data: null }),
  },

  // ========== Schedules ==========
  {
    url: '/api/schedules',
    method: 'get',
    response: ({ query }: any) => {
      let result = schedules
      if (query.date) result = result.filter((s) => s.workDate === query.date)
      if (query.deptId) result = result.filter((s) => doctors.find((d) => d.id === s.doctorId)?.departmentId === Number(query.deptId))
      return { code: 200, message: 'success', data: result }
    },
  },
  {
    url: '/api/schedules/available',
    method: 'get',
    response: ({ query }: any) => {
      let result = schedules.filter((s) => s.remainSlots > 0)
      if (query.date) result = result.filter((s) => s.workDate === query.date)
      return { code: 200, message: 'success', data: result }
    },
  },
  {
    url: '/api/schedules',
    method: 'post',
    response: ({ body }: any) => {
      const doc = doctors.find((d) => d.id === body.doctorId)
      const item = { id: ++nextId, ...body, doctorName: doc?.name || '', departmentName: doc?.departmentName || '' }
      schedules.push(item as any)
      return { code: 200, message: 'success', data: item }
    },
  },
  {
    url: '/api/schedules',
    method: 'put',
    response: ({ body }: any) => ({ code: 200, message: 'success', data: body }),
  },
  {
    url: '/api/schedules',
    method: 'delete',
    response: () => ({ code: 200, message: 'success', data: null }),
  },

  // ========== Registrations ==========
  {
    url: '/api/registrations',
    method: 'get',
    response: ({ query }: any) => {
      let result = registrations
      if (query.status !== undefined) result = result.filter((r) => r.status === Number(query.status))
      return { code: 200, message: 'success', data: result }
    },
  },
  {
    url: '/api/registrations',
    method: 'post',
    response: ({ body }: any) => {
      const sched = schedules.find((s) => s.id === body.scheduleId)
      const pat = patients.find((p) => p.id === body.patientId)
      if (sched) sched.remainSlots--
      const item = { id: ++nextId, ...body, status: 0, seqNo: registrations.length + 1, fee: 10.00, regTime: new Date().toISOString(), patientName: pat?.name || '', doctorName: sched?.doctorName || '', departmentName: sched?.departmentName || '', timeSlot: sched?.timeSlot || '' }
      registrations.push(item as any)
      return { code: 200, message: 'success', data: item }
    },
  },
  {
    url: /\/api\/registrations\/\d+\/call$/,
    method: 'put',
    response: () => ({ code: 200, message: 'success', data: null }),
  },
  {
    url: /\/api\/registrations\/\d+\/complete$/,
    method: 'put',
    response: () => ({ code: 200, message: 'success', data: null }),
  },
  {
    url: /\/api\/registrations\/\d+$/,
    method: 'delete',
    response: () => ({ code: 200, message: 'success', data: null }),
  },

  // ========== Medical Records ==========
  {
    url: '/api/medical-records',
    method: 'get',
    response: () => ({ code: 200, message: 'success', data: medicalRecords }),
  },
  {
    url: '/api/medical-records',
    method: 'post',
    response: ({ body }: any) => {
      const item = { id: ++nextId, ...body, patientName: patients.find((p) => p.id === body.patientId)?.name || '', doctorName: doctors.find((d) => d.id === body.doctorId)?.name || '' }
      medicalRecords.push(item as any)
      return { code: 200, message: 'success', data: item }
    },
  },
  {
    url: '/api/medical-records',
    method: 'put',
    response: ({ body }: any) => ({ code: 200, message: 'success', data: body }),
  },

  // ========== Medicines ==========
  {
    url: '/api/medicines',
    method: 'get',
    response: ({ query }: any) => {
      let result = medicines.filter((m) => m.status === 1)
      if (query.keyword) {
        const kw = query.keyword.toLowerCase()
        result = result.filter((m) => m.name.toLowerCase().includes(kw) || m.pinyinCode?.toLowerCase().includes(kw))
      }
      return { code: 200, message: 'success', data: result }
    },
  },
  {
    url: '/api/medicines/low-stock',
    method: 'get',
    response: () => ({ code: 200, message: 'success', data: medicines.filter((m) => m.stock <= m.safetyStock && m.status === 1) }),
  },
  {
    url: '/api/medicines',
    method: 'post',
    response: ({ body }: any) => {
      const item = { id: ++nextId, ...body }
      medicines.push(item as any)
      return { code: 200, message: 'success', data: item }
    },
  },
  {
    url: '/api/medicines',
    method: 'put',
    response: ({ body }: any) => ({ code: 200, message: 'success', data: body }),
  },
  {
    url: /\/api\/medicines\/\d+\/stock-in$/,
    method: 'post',
    response: () => ({ code: 200, message: 'success', data: null }),
  },
  {
    url: /\/api\/medicines\/\d+\/stock-out$/,
    method: 'post',
    response: () => ({ code: 200, message: 'success', data: null }),
  },
  {
    url: /\/api\/medicines\/\d+$/,
    method: 'delete',
    response: () => ({ code: 200, message: 'success', data: null }),
  },

  // ========== Prescriptions ==========
  {
    url: '/api/prescriptions',
    method: 'get',
    response: () => ({ code: 200, message: 'success', data: prescriptions }),
  },
  {
    url: '/api/prescriptions',
    method: 'post',
    response: ({ body }: any) => {
      const item = { id: ++nextId, ...body.prescription, items: body.items, status: 0, patientName: patients.find((p) => p.id === body.prescription.patientId)?.name || '', doctorName: doctors.find((d) => d.id === body.prescription.doctorId)?.name || '' }
      prescriptions.push(item as any)
      return { code: 200, message: 'success', data: item }
    },
  },
  {
    url: /\/api\/prescriptions\/\d+\/dispense$/,
    method: 'put',
    response: () => ({ code: 200, message: 'success', data: null }),
  },
  {
    url: /\/api\/prescriptions\/\d+\/cancel$/,
    method: 'put',
    response: () => ({ code: 200, message: 'success', data: null }),
  },
  {
    url: /\/api\/prescriptions\/by-record\/\d+$/,
    method: 'get',
    response: () => ({ code: 200, message: 'success', data: prescriptions[0] || null }),
  },

  // ========== Users ==========
  {
    url: '/api/users',
    method: 'get',
    response: () => ({ code: 200, message: 'success', data: users }),
  },
  {
    url: '/api/users',
    method: 'post',
    response: ({ body }: any) => {
      const item = { id: ++nextId, ...body }
      users.push(item as any)
      return { code: 200, message: 'success', data: item }
    },
  },
  {
    url: '/api/users',
    method: 'put',
    response: ({ body }: any) => ({ code: 200, message: 'success', data: body }),
  },
  {
    url: /\/api\/users\/\d+\/password$/,
    method: 'put',
    response: () => ({ code: 200, message: 'success', data: null }),
  },
  {
    url: /\/api\/users\/\d+$/,
    method: 'delete',
    response: () => ({ code: 200, message: 'success', data: null }),
  },
] as MockMethod[]