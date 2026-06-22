const puppeteer = require('puppeteer');
const fs = require('fs');
const path = require('path');

async function generatePdf() {
    const { marked } = await import('marked');
    
    const inputPath = path.join(__dirname, 'DOC', '部署手册.md');
    const outputPath = path.join(__dirname, 'DOC', '部署手册.pdf');

    const markdownContent = fs.readFileSync(inputPath, 'utf-8');

    const htmlContent = `<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>MediCare 智慧医疗门诊管理系统 - 部署手册</title>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
            max-width: 210mm;
            margin: 0 auto;
            padding: 20mm;
            font-size: 14px;
            line-height: 1.6;
            color: #333;
        }
        h1 {
            font-size: 24px;
            text-align: center;
            border-bottom: 2px solid #333;
            padding-bottom: 10px;
            margin-bottom: 20px;
        }
        h2 {
            font-size: 20px;
            color: #1a73e8;
            margin-top: 30px;
            margin-bottom: 15px;
            padding-bottom: 5px;
            border-bottom: 1px solid #eee;
        }
        h3 {
            font-size: 16px;
            margin-top: 20px;
            margin-bottom: 10px;
            color: #333;
        }
        table {
            width: 100%;
            border-collapse: collapse;
            margin: 15px 0;
            font-size: 13px;
        }
        table th, table td {
            border: 1px solid #ddd;
            padding: 8px 12px;
            text-align: left;
        }
        table th {
            background-color: #f5f5f5;
            font-weight: 600;
        }
        table tr:nth-child(even) {
            background-color: #fafafa;
        }
        code {
            background-color: #f4f4f4;
            padding: 2px 6px;
            border-radius: 3px;
            font-family: 'Monaco', 'Menlo', monospace;
            font-size: 12px;
            color: #c7254e;
        }
        pre {
            background-color: #f5f5f5;
            padding: 15px;
            border-radius: 6px;
            overflow-x: auto;
            margin: 15px 0;
            border: 1px solid #eee;
        }
        pre code {
            background-color: transparent;
            padding: 0;
            color: #333;
            font-size: 12px;
        }
        p {
            margin: 10px 0;
        }
        ul, ol {
            margin: 10px 0 10px 25px;
            padding-left: 10px;
        }
        li {
            margin: 5px 0;
        }
        a {
            color: #1a73e8;
            text-decoration: none;
        }
        a:hover {
            text-decoration: underline;
        }
        img {
            max-width: 100%;
            height: auto;
            margin: 10px 0;
            border-radius: 4px;
        }
        hr {
            border: none;
            border-top: 1px solid #eee;
            margin: 30px 0;
        }
        .toc {
            margin-bottom: 30px;
            padding: 15px;
            background-color: #f9f9f9;
            border-radius: 6px;
        }
        .toc ul {
            list-style: none;
            margin: 0;
            padding: 0;
        }
        .toc li {
            margin: 5px 0;
        }
    </style>
</head>
<body>
${marked.parse(markdownContent)}
</body>
</html>`;

    const browser = await puppeteer.launch({
        headless: true,
        args: ['--no-sandbox', '--disable-setuid-sandbox']
    });

    const page = await browser.newPage();

    await page.setContent(htmlContent, {
        waitUntil: 'networkidle0',
        timeout: 30000
    });

    await page.setViewport({
        width: 1200,
        height: 800
    });

    await page.pdf({
        path: outputPath,
        format: 'A4',
        printBackground: true,
        margin: {
            top: '20mm',
            right: '15mm',
            bottom: '20mm',
            left: '15mm'
        }
    });

    await browser.close();

    console.log('PDF generated successfully:', outputPath);
}

generatePdf().catch(err => {
    console.error('Error generating PDF:', err);
    process.exit(1);
});
