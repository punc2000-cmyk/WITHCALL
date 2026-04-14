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
      return new Response(
        JSON.stringify({ error: '단말기 데이터가 없습니다' }),
        { status: 400, headers: CORS }
      )
    }

    // 배치 생성
    const batch = await env.DB.prepare(
      'INSERT INTO batches (name, total_count) VALUES (?, ?) RETURNING id'
    ).bind(batchName || new Date().toISOString(), devices.length).first()

    const batchId = batch.id

    // 단말기 일괄 등록
    const stmt = env.DB.prepare(`
      INSERT INTO devices (
        batch_id,
        region, school_name, address,
        phone_119, registered_119,
        phone_care, registered_care,
        contact_1, contact_2, contact_3, contact_4, contact_5
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    `)

    const inserts = devices.map(d =>
      stmt.bind(
        batchId,
        d.region         || '',
        d.schoolName     || '',
        d.address        || '',
        d.phone119       || '',
        d.registered119  || '',
        d.phoneCare      || '',
        d.registeredCare || '',
        d.contact1       || '',
        d.contact2       || '',
        d.contact3       || '',
        d.contact4       || '',
        d.contact5       || ''
      )
    )

    await env.DB.batch(inserts)

    return new Response(
      JSON.stringify({ success: true, batchId, count: devices.length }),
      { headers: CORS }
    )
  } catch (e) {
    return new Response(
      JSON.stringify({ error: e.message }),
      { status: 500, headers: CORS }
    )
  }
}
