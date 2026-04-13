const CORS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'PATCH, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type',
  'Content-Type': 'application/json'
}

export async function onRequestOptions() {
  return new Response(null, { headers: CORS })
}

export async function onRequestPatch(context) {
  const { params, env } = context
  const { id } = params

  try {
    await env.DB.prepare(
      "UPDATE devices SET status = 'completed', completed_at = CURRENT_TIMESTAMP WHERE id = ?"
    ).bind(id).run()

    return new Response(JSON.stringify({ success: true }), { headers: CORS })
  } catch (e) {
    return new Response(JSON.stringify({ error: e.message }), { status: 500, headers: CORS })
  }
}
