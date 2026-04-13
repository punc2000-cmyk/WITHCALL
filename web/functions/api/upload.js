const CORS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'POST, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type',
  'Content-Type': 'application/json'
}

export async function onRequestOptions() {
  return new Response(null, { headers: CORS })
}

export async function onRequestPost(context) {
  const { request, env } = context
  try {
    const { devices, batchName } = await request.json()

    if (!devices || devices.length === 0) {
      return new Response(JSON.stringify({ error: '단말기 데이터가 없습니다' }), { status: 400, headers: CORS })
    }

    // 배치 생성
    const batch = await env.DB.prepare(
      'INSERT INTO batches (name, total_count) VALUES (?, ?) RETURNING id'
    ).bind(batchName || new Date().toISOString(), devices.length).first()

    const batchId = batch.id

    // 단말기 일괄 등록
    const stmt = env.DB.prepare(
      'INSERT INTO devices (batch_id, phone_number, msg_b, msg_c, msg_d, msg_e, msg_f) VALUES (?, ?, ?, ?, ?, ?, ?)'
    )

    const inserts = devices.map(d =>
      stmt.bind(batchId, d.phoneNumber, d.msgB || '', d.msgC || '', d.msgD || '', d.msgE || '', d.msgF || '')
    )

    await env.DB.batch(inserts)

    return new Response(JSON.stringify({ success: true, batchId, count: devices.length }), { headers: CORS })
  } catch (e) {
    return new Response(JSON.stringify({ error: e.message }), { status: 500, headers: CORS })
  }
}
