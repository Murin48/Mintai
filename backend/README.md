# 🛡️ LeaseCheck API - Node.js Backend for Render

บริการประมวลผลสัญญาเช่าหอพักอัตโนมัติจากไฟล์ PDF และวิเคราะห์ความสอดคล้องตามกฎหมาย **สคบ. 2562** แบ่งระดับสี 3 ระดับ (แดง, เหลือง, เขียว)

## 🚀 วิธีการ Deploy บน Render (ภายใน 2 นาที)

### วิธีที่ 1: Deploy ผ่าน Render Blueprint (แนะนำ)
1. นำโฟลเดอร์นี้ขึ้น GitHub Repository ของคุณ
2. เข้าสู่เว็บไซต์ [Render.com](https://render.com)
3. เลือก **New +** -> **Blueprint**
4. เลือก Git Repository ของคุณ ระบบจะอ่านไฟล์ `render.yaml` และตั้งค่าทั้งหมดให้อัตโนมัติ!
5. กด **Apply** เพื่อเริ่มการ deploy

### วิธีที่ 2: Deploy ผ่าน Web Service ธรรมดา
1. สร้าง **New Web Service** บน Render
2. กำหนดค่าดังนี้:
   - **Environment:** `Node`
   - **Build Command:** `npm install`
   - **Start Command:** `node server.js`
   - **Health Check Path:** `/health`
3. กด **Create Web Service**

---

## 📡 API Endpoints

### 1. `POST /api/analyze-pdf`
อัปโหลดไฟล์ PDF ของสัญญาเช่า เพื่อให้ระบบแปลงข้อความและตรวจจับข้อสัญญาไม่เป็นธรรมโดยอัตโนมัติ
- **Content-Type:** `multipart/form-data`
- **Body Field:** `file` (ไฟล์ PDF สัญญาเช่า)

**ตัวอย่างการทดสอบด้วย cURL:**
```bash
curl -X POST https://your-app.onrender.com/api/analyze-pdf \
  -F "file=@contract.pdf"
```

### 2. `POST /api/analyze-text`
ส่งข้อความสัญญาเช่าในรูปแบบ JSON
```json
{
  "contractText": "ข้อ 1. ผู้เช่าจ่ายค่าไฟหน่วยละ 8 บาท และเงินประกัน 2 เดือน..."
}
```

### 3. `GET /api/rules`
ดึงข้อมูลข้อห้ามและข้อกำหนดตามประกาศ สคบ. 2562

### 4. `GET /health`
ตรวจสอบสถานะของ API Service
