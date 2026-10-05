/**
 * LeaseCheck API Server (Node.js + Express)
 * ตรวจสอบสัญญาเช่าหอพักตามกฎหมาย สคบ. 2562
 * รองรับไฟล์ PDF และจำแนก 3 ระดับสี: แดง, เหลือง, เขียว
 * ออกแบบมาเพื่อ deploy บน Render.com ได้ทันที
 */

const express = require('express');
const cors = require('cors');
const multer = require('multer');
const pdfParse = require('pdf-parse');
require('dotenv').config();

const app = express();
const port = process.env.PORT || 3000;

// Enable CORS and JSON body parser
app.use(cors());
app.use(express.json());

// Memory storage for PDF uploads
const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 10 * 1024 * 1024 } // Max 10MB
});

// Health check endpoint for Render
app.get('/health', (req, res) => {
  res.json({ status: 'ok', service: 'LeaseCheck API', timestamp: new Date().toISOString() });
});

app.get('/', (req, res) => {
  res.send(`
    <html>
      <head><title>LeaseCheck API - สคบ. 2562</title></head>
      <body style="font-family: sans-serif; padding: 40px; line-height: 1.6; max-width: 800px; margin: auto;">
        <h1>🛡️ LeaseCheck API Service</h1>
        <p>บริการตรวจสอบสัญญาเช่าหอพักตามประกาศ สคบ. พ.ศ. 2562 แบ่งตามสี 3 ระดับ (แดง, เหลือง, เขียว)</p>
        <ul>
          <li><code>POST /api/analyze-pdf</code> - อัปโหลดไฟล์ PDF (form-data: file)</li>
          <li><code>POST /api/analyze-text</code> - ส่งข้อความสัญญา JSON <code>{"contractText": "..."}</code></li>
          <li><code>GET /api/rules</code> - รายการข้อห้ามและสิทธิตามกฎหมาย สคบ. 2562</li>
          <li><code>GET /health</code> - ตรวจสอบสถานะการทำงาน (Render Health Check)</li>
        </ul>
      </body>
    </html>
  `);
});

// Core OCPB 2562 Analysis Function
function evaluateOcpbCompliance(text) {
  const findings = [];
  const normalized = text.toLowerCase();

  // 1. ค่าไฟฟ้า
  const elecMatch = normalized.match(/(?:ค่าไฟ|ไฟฟ้า)[^\n]{0,40}?(?:หน่วยละ|ยูนิตละ|อัตรา)\s*([6-9]|1[0-9])\s*(?:บาท)?/);
  if (elecMatch || normalized.includes('ค่าไฟหน่วยละ 7') || normalized.includes('ค่าไฟหน่วยละ 8') || normalized.includes('ค่าไฟหน่วยละ 9')) {
    findings.push({
      id: 'elec_red',
      category: 'ค่าน้ำ-ค่าไฟ',
      title: 'เรียกเก็บค่าไฟเกินอัตราการไฟฟ้า',
      level: 'RED',
      lawArticle: 'ประกาศ สคบ. 2562 ข้อ 5',
      snippet: elecMatch ? elecMatch[0] : 'ระบุค่าไฟเกินจริง',
      explanation: 'ห้ามเรียกเก็บค่าไฟฟ้าเกินกว่าอัตราที่การไฟฟ้านครหลวงหรือส่วนภูมิภาคเรียกเก็บจริง',
      advice: 'ขอแก้ไขให้คิดตามบิลการไฟฟ้าจริง (~4.2 บ./หน่วย)'
    });
  } else if (normalized.includes('ตามอัตราการไฟฟ้า') || normalized.includes('ตามบิลการไฟฟ้า')) {
    findings.push({
      id: 'elec_green',
      category: 'ค่าน้ำ-ค่าไฟ',
      title: 'ค่าไฟฟ้าคิดตามอัตราจริงของการไฟฟ้า',
      level: 'GREEN',
      lawArticle: 'ประกาศ สคบ. 2562 ข้อ 3 (2)',
      snippet: 'คิดตามอัตราการไฟฟ้า',
      explanation: 'ถูกต้องตามเกณฑ์ สคบ. 2562',
      advice: 'ตรวจสอบบิลการไฟฟ้าจริงควบคู่กับมิเตอร์'
    });
  }

  // 2. ค่าน้ำประปา
  const waterMatch = normalized.match(/(?:ค่าน้ำ|ประปา)[^\n]{0,40}?(?:หน่วยละ|ยูนิตละ|อัตรา)\s*(1[8-9]|2[0-9]|3[0-9])\s*(?:บาท)?/);
  if (waterMatch || normalized.includes('ค่าน้ำหน่วยละ 18') || normalized.includes('ค่าน้ำหน่วยละ 20') || normalized.includes('เหมาค่าน้ำเดือนละ')) {
    findings.push({
      id: 'water_red',
      category: 'ค่าน้ำ-ค่าไฟ',
      title: 'เรียกเก็บค่าน้ำประปาเกินจริง / เหมาจ่ายเกินอัตรา',
      level: 'RED',
      lawArticle: 'ประกาศ สคบ. 2562 ข้อ 5',
      snippet: waterMatch ? waterMatch[0] : 'ระบุค่าน้ำเกินอัตราการประปา',
      explanation: 'ห้ามเรียกเก็บค่าน้ำเกินอัตราการประปานครหลวง/ภูมิภาค',
      advice: 'ขอแก้ไขให้คิดตามอัตราการประปาจริง'
    });
  }

  // 3. เงินประกัน
  if (normalized.includes('เงินประกัน 2 เดือน') || normalized.includes('เงินประกัน 3 เดือน') || normalized.includes('มัดจำ 2 เดือน') || normalized.includes('มัดจำ 3 เดือน')) {
    findings.push({
      id: 'deposit_red',
      category: 'เงินประกันและค่าเช่าล่วงหน้า',
      title: 'เรียกเก็บเงินประกันเกินกว่า 1 เดือน',
      level: 'RED',
      lawArticle: 'ประกาศ สคบ. 2562 ข้อ 5',
      snippet: 'เงินประกัน 2-3 เดือน',
      explanation: 'ห้ามเรียกเก็บเงินประกันเกิน 1 เดือนของอัตราค่าเช่ารายเดือน',
      advice: 'จ่ายเพียง 1 เดือนตามสิทธิทางกฎหมาย'
    });
  } else if (normalized.includes('เงินประกัน 1 เดือน') || normalized.includes('มัดจำ 1 เดือน')) {
    findings.push({
      id: 'deposit_green',
      category: 'เงินประกันและค่าเช่าล่วงหน้า',
      title: 'เงินประกันไม่เกิน 1 เดือน',
      level: 'GREEN',
      lawArticle: 'ประกาศ สคบ. 2562 ข้อ 4 (1)',
      snippet: 'เงินประกัน 1 เดือน',
      explanation: 'ถูกต้องตามเกณฑ์ สคบ. 2562',
      advice: 'ขอใบเสร็จรับเงินไว้เป็นหลักฐาน'
    });
  }

  // 4. ล็อคห้อง / ตัดน้ำไฟ / ยึดทรัพย์สิน
  if (normalized.includes('ล็อคห้อง') || normalized.includes('ล็อกห้อง') || normalized.includes('ยึดทรัพย์สิน') || normalized.includes('ตัดน้ำตัดไฟ') || normalized.includes('ขนย้ายทรัพย์สิน')) {
    findings.push({
      id: 'lockout_red',
      category: 'การล็อคห้องและยึดทรัพย์สิน',
      title: 'ล็อคห้อง ตัดน้ำไฟ หรือยึดทรัพย์สินโดยพลการ (ผิดกฎหมายร้ายแรง)',
      level: 'RED',
      lawArticle: 'ประกาศ สคบ. 2562 ข้อ 5',
      snippet: 'ล็อคห้อง/ตัดน้ำไฟ/ยึดทรัพย์สิน',
      explanation: 'การล็อคห้องหรือยึดทรัพย์สินเป็นความผิดทางอาญาฐานบุกรุกและลักทรัพย์ สัญญาข้อนี้ตกเป็นโมฆะ',
      advice: 'ผู้ให้เช่าไม่มีสิทธิทำได้ หากเกิดขึ้นสามารถแจ้งความดำเนินคดีได้ทันที'
    });
  }

  // 5. เข้าตรวจห้องพักโดยไม่แจ้ง
  if (normalized.includes('เข้าตรวจได้ตลอดเวลาโดยไม่ต้องแจ้ง') || normalized.includes('โดยมิต้องบอกล่วงหน้า')) {
    findings.push({
      id: 'entry_red',
      category: 'การเข้าตรวจห้องพัก',
      title: 'สงวนสิทธิเข้าตรวจห้องพักโดยไม่แจ้งล่วงหน้า',
      level: 'RED',
      lawArticle: 'ประกาศ สคบ. 2562 ข้อ 5',
      snippet: 'เข้าตรวจได้โดยมิต้องบอกล่วงหน้า',
      explanation: 'ต้องแจ้งล่วงหน้าไม่น้อยกว่า 24 ชม. และได้รับความยินยอมจากผู้เช่า',
      advice: 'ขอแก้ไขให้ต้องนัดหมายล่วงหน้า'
    });
  }

  // 6. การบอกเลิกสัญญา
  if (normalized.includes('ห้ามบอกเลิกสัญญา') || normalized.includes('ริบเงินประกันทันทีหากอยู่ไม่ครบ')) {
    findings.push({
      id: 'term_red',
      category: 'การบอกเลิกสัญญา',
      title: 'ตัดสิทธิบอกเลิกสัญญา / ริบเงินประกันทั้งหมด',
      level: 'RED',
      lawArticle: 'ประกาศ สคบ. 2562 ข้อ 4 (6) และ ข้อ 5',
      snippet: 'ริบเงินประกันหากย้ายออกก่อนครบกำหนด',
      explanation: 'ผู้เช่ามีสิทธิบอกเลิกสัญญาได้โดยบอกกล่าวล่วงหน้าไม่น้อยกว่า 30 วัน',
      advice: 'ขอเพิ่มสิทธิบอกเลิกสัญญาล่วงหน้า 30 วัน'
    });
  } else if (normalized.includes('แจ้งย้ายออกล่วงหน้า 60 วัน') || normalized.includes('บอกกล่าวก่อน 60 วัน')) {
    findings.push({
      id: 'term_yellow',
      category: 'การบอกเลิกสัญญา',
      title: 'กำหนดระยะเวลาแจ้งล่วงหน้านานเกินควร (60 วัน)',
      level: 'YELLOW',
      lawArticle: 'ประกาศ สคบ. 2562 ข้อ 4 (6)',
      snippet: 'แจ้งย้ายออกล่วงหน้า 60 วัน',
      explanation: 'เกณฑ์มาตรฐาน สคบ. กำหนดไว้ 30 วัน',
      advice: 'เจรจาปรับลดเหลือ 30 วัน'
    });
  } else if (normalized.includes('บอกกล่าวล่วงหน้าไม่น้อยกว่า 30 วัน') || normalized.includes('แจ้งล่วงหน้า 30 วัน')) {
    findings.push({
      id: 'term_green',
      category: 'การบอกเลิกสัญญา',
      title: 'สิทธิบอกเลิกสัญญาล่วงหน้า 30 วัน ถูกต้องตามกฎหมาย',
      level: 'GREEN',
      lawArticle: 'ประกาศ สคบ. 2562 ข้อ 4 (6)',
      snippet: 'บอกกล่าวล่วงหน้าไม่น้อยกว่า 30 วัน',
      explanation: 'ถูกต้องตามเกณฑ์ สคบ. 2562',
      advice: 'ทำหนังสือแจ้งเป็นลายลักษณ์อักษรเมื่อจะย้ายออก'
    });
  }

  // 7. การคืนเงินประกัน
  if (normalized.includes('ภายใน 45 วัน') || normalized.includes('ภายใน 60 วัน')) {
    findings.push({
      id: 'refund_yellow',
      category: 'การคืนเงินประกัน',
      title: 'ระยะเวลาคืนเงินประกันยาวนานเกิน 7 วัน',
      level: 'YELLOW',
      lawArticle: 'ประกาศ สคบ. 2562 ข้อ 4 (7)',
      snippet: 'คืนเงินประกันภายใน 45-60 วัน',
      explanation: 'สคบ. กำหนดให้คืนเงินประกันทันทีหรือไม่เกิน 7 วันหลังส่งมอบห้อง',
      advice: 'ขอปรับระยะเวลาเป็น 7 วันตามเกณฑ์'
    });
  } else if (normalized.includes('คืนเงินประกันภายใน 7 วัน')) {
    findings.push({
      id: 'refund_green',
      category: 'การคืนเงินประกัน',
      title: 'คืนเงินประกันภายใน 7 วัน ถูกต้องตามกฎหมาย',
      level: 'GREEN',
      lawArticle: 'ประกาศ สคบ. 2562 ข้อ 4 (7)',
      snippet: 'คืนเงินประกันภายใน 7 วัน',
      explanation: 'ถูกต้องตามเกณฑ์ สคบ. 2562',
      advice: 'ตรวจห้องพร้อมผู้ให้เช่าในวันส่งมอบ'
    });
  }

  // 8. ค่าต่อสัญญา
  if (normalized.includes('ค่าต่อสัญญา') || normalized.includes('ค่าธรรมเนียมต่อสัญญา')) {
    findings.push({
      id: 'renewal_red',
      category: 'ค่าต่อสัญญา',
      title: 'เรียกเก็บค่าต่อสัญญาเช่า',
      level: 'RED',
      lawArticle: 'ประกาศ สคบ. 2562 ข้อ 5',
      snippet: 'มีค่าต่อสัญญาเช่า',
      explanation: 'ห้ามเรียกเก็บค่าต่อสัญญาเช่าใดๆ ทั้งสิ้น',
      advice: 'ปฏิเสธการจ่ายตามประกาศ สคบ.'
    });
  }

  const redCount = findings.filter(f => f.level === 'RED').length;
  const yellowCount = findings.filter(f => f.level === 'YELLOW').length;
  const greenCount = findings.filter(f => f.level === 'GREEN').length;

  const safetyScore = Math.max(0, Math.min(100, 100 - (redCount * 25) - (yellowCount * 10)));

  return {
    safetyScore,
    redCount,
    yellowCount,
    greenCount,
    findings,
    verdict: redCount > 0
      ? `พบข้อสัญญาผิดกฎหมาย สคบ. 2562 (ระดับสีแดง ${redCount} ข้อ)`
      : yellowCount > 0
      ? `ผ่านเกณฑ์พื้นฐานแต่มีข้อควรระวัง (ระดับสีเหลือง ${yellowCount} ข้อ)`
      : 'สัญญาถูกต้อง เป็นธรรม สอดคล้องตามประกาศ สคบ. 2562'
  };
}

// Endpoint: Upload PDF and Analyze
app.post('/api/analyze-pdf', upload.single('file'), async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ error: 'กรุณาอัปโหลดไฟล์ PDF (field name: "file")' });
    }

    const pdfBuffer = req.file.buffer;
    const data = await pdfParse(pdfBuffer);
    const extractedText = data.text;

    const analysis = evaluateOcpbCompliance(extractedText);

    return res.json({
      success: true,
      fileName: req.file.originalname,
      totalPages: data.numpages,
      textLength: extractedText.length,
      extractedExcerpt: extractedText.substring(0, 300),
      ...analysis
    });
  } catch (error) {
    console.error('PDF parsing error:', error);
    return res.status(500).json({
      success: false,
      error: 'เกิดข้อผิดพลาดในการประมวลผล PDF: ' + error.message
    });
  }
});

// Endpoint: Analyze Text Directly
app.post('/api/analyze-text', (req, res) => {
  const { contractText } = req.body;
  if (!contractText || typeof contractText !== 'string') {
    return res.status(400).json({ error: 'กรุณาระบุ contractText ใน JSON body' });
  }

  const analysis = evaluateOcpbCompliance(contractText);
  return res.json({
    success: true,
    textLength: contractText.length,
    ...analysis
  });
});

// Endpoint: Get OCPB 2562 Rules
app.get('/api/rules', (req, res) => {
  res.json({
    lawName: 'ประกาศคณะกรรมการว่าด้วยสัญญา เรื่อง ให้ธุรกิจการให้เช่าอาคารเพื่ออยู่อาศัยเป็นธุรกิจที่ควบคุมสัญญา พ.ศ. 2562',
    enactedDate: '1 พฤษภาคม 2562',
    keyRules: [
      { rule: 'ห้ามเก็บค่าน้ำ-ค่าไฟเกินอัตราจริง', level: 'RED', penalty: 'จำคุกไม่เกิน 1 ปี หรือปรับไม่เกิน 100,000 บาท' },
      { rule: 'ห้ามเรียกเก็บเงินประกันเกิน 1 เดือน', level: 'RED' },
      { rule: 'ห้ามเรียกเก็บค่าเช่าล่วงหน้าเกิน 1 เดือน', level: 'RED' },
      { rule: 'ห้ามล็อคห้อง ยึดทรัพย์สิน หรือตัดน้ำไฟเมื่อผู้เช่าค้างค่าเช่า', level: 'RED' },
      { rule: 'ห้ามเข้าตรวจห้องพักโดยไม่แจ้งล่วงหน้า', level: 'RED' },
      { rule: 'ห้ามเรียกเก็บค่าต่อสัญญาเช่า', level: 'RED' },
      { rule: 'ต้องคืนเงินประกันทันทีหรือไม่เกิน 7 วันหลังย้ายออก', level: 'GREEN' },
      { rule: 'ผู้เช่ามีสิทธิบอกเลิกสัญญาแจ้งล่วงหน้า 30 วัน', level: 'GREEN' }
    ],
    contactOcpb: {
      hotline: '1166',
      website: 'https://www.ocpb.go.th',
      onlineComplain: 'https://complaint.ocpb.go.th'
    }
  });
});

app.listen(port, () => {
  console.log(`LeaseCheck API server running on port ${port}`);
});
