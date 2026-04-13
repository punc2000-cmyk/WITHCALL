const CORS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type',
  'Content-Type': 'application/json'
}

export async function onRequestOptions() {
  return new Response(null, { headers: CORS })
}

export async function onRequestGet(context) {
  const { request, env } = context
  const url = new URL(request.url)
  const batchId = url.searchParams.get('batch_id')
  const status = url.searchParams.get('status') // pending | completed | all(기본)

  try {
    let results

    // 배치 ID가 없으면 최신 배치 사용
    let targetBatchId = batchId
    if (!targetBatchId) {
      const latest = await env.DB.prepare(
        'SELECT id FROM batches ORDER BY created_at DESC LIMIT 1'
      ).first()
      if (!latest) {
        return new Response(JSON.stringify([]), { headers: CORS })
      }
      targetBatchId = latest.id
    }

    if (status === 'pending' || status === 'completed') {
      results = await env.DB.prepare(
        'SELECT * FROM devices WHERE batch_id = ? AND status = ? ORDER BY id'
      ).bind(targetBatchId, status).all()
    } else {
      results = await env.DB.prepare(
        'SELECT * FROM devices WHERE batch_id = ? ORDER BY id'
      ).bind(targetBatchId).all()
    }

    return new Response(JSON.stringify(results.results), { headers: CORS })
  } catch (e) {
    return new Response(JSON.stringify({ error: e.message }), { status: 500, headers: CORS })
  }
}
