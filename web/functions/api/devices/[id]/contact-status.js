const CORS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'PATCH, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type',
  'Content-Type': 'application/json'
}

export async function onRequestOptions() {
  return new Response(null, { headers: CORS })
}

/**
 * PATCH /api/devices/:id/contact-status
 * Body: {
 *   contactIndex : 1~5,
 *   status       : "변경완료" | "에러" | "",
 *   phase        : "119" | "care"
 * }
 */
export async function onRequestPatch(context) {
  const { params, env, request } = context
  const { id } = params

  try {
    const { contactIndex, status, phase } = await request.json()

    if (![1, 2, 3, 4, 5].includes(contactIndex)) {
      return new Response(
        JSON.stringify({ error: 'contactIndex는 1~5 사이여야 합니다' }),
        { status: 400, headers: CORS }
      )
    }
    if (!['119', 'care'].includes(phase)) {
      return new Response(
        JSON.stringify({ error: 'phase는 "119" 또는 "care" 여야 합니다' }),
        { status: 400, headers: CORS }
      )
    }
    if (!['변경완료', '에러', ''].includes(status)) {
      return new Response(
        JSON.stringify({ error: '유효하지 않은 status 값입니다' }),
        { status: 400, headers: CORS }
      )
    }

    const col = `contact_${contactIndex}_status_${phase}`
    await env.DB.prepare(
      `UPDATE devices SET ${col} = ? WHERE id = ?`
    ).bind(status, id).run()

    return new Response(JSON.stringify({ success: true }), { headers: CORS })
  } catch (e) {
    return new Response(
      JSON.stringify({ error: e.message }),
      { status: 500, headers: CORS }
    )
  }
}
